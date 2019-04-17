/*
 * Code modifié du FrameWork BioJAVA
 */
package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.DataSource;
import org.biojava.nbio.core.sequence.TaxonomyID;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.DBReferenceInfo;
import org.biojava.nbio.core.sequence.io.GenbankSequenceParser;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Use GenbankReaderHelper as an example of how to use this class where GenbankReaderHelper should be the
 * primary class used to read Genbank files
 *
 * Classe GenbankReader modifiée
 */
public class CustomGenbankReader extends AbstractCustomReader{

    private GenbankSequenceParser<DNASequence, NucleotideCompound> genbankParser;
    private BufferedReader bufferedReader;
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGenbankReader.class);

    /**
     * If you are going to use the FileProxyProteinSequenceCreator then you
     * need to use this constructor because we need details about
     * the location of the file.
     * @param file fichier à lire
     * @throws FileNotFoundException if the file does not exist, is a directory
     * 	rather than a regular file, or for some other reason cannot be opened
     * 	for reading.
     * @throws SecurityException if a security manager exists and its checkRead
     * 	method denies read access to the file.
     */
    CustomGenbankReader(final File file ) throws FileNotFoundException {
        super(file);
        this.bufferedReader = new BufferedReader(new FileReader(file));
        genbankParser = new GenbankSequenceParser<>();
    }

    /**
     * The parsing is done in this method.<br>
     * This method tries to process all the available Genbank records
     * in the File or InputStream, closes the underlying resource,
     * and return the results in {@link LinkedHashMap}.<br>
     * You don't need to call {@link #close()} after calling this method.
     * @see #process(int)
     * @return {@link HashMap} containing all the parsed Genbank records
     * present, starting current fileIndex onwards.
     * @throws IOException
     */
    LinkedHashMap<String, DNASequence> process() throws IOException {
        return process(-1);
    }

    /**
     * This method tries to parse maximum <code>max</code> records from
     * the open File or InputStream, and leaves the underlying resource open.<br>
     *
     * Subsequent calls to the same method continue parsing the rest of the file.<br>
     * This is particularly useful when dealing with very big data files,
     * (e.g. NCBI nr database), which can't fit into memory and will take long
     * time before the first result is available.<br>
     * <b>N.B.</b>
     * <ul>
     * <li>This method can't be called after calling its NO-ARGUMENT twin.</li>
     * <li>remember to close the underlying resource when you are done.</li>
     * </ul>
     * @see #process()
     * @author Amr AL-Hossary
     * @since 3.0.6
     * @param max maximum number of records to return, <code>-1</code> for infinity.
     * @return {@link HashMap} containing maximum <code>max</code> parsed Genbank records
     * present, starting current fileIndex onwards.
     * @throws IOException
     */
    LinkedHashMap<String, DNASequence> process(final int max) throws IOException {
        LinkedHashMap<String, DNASequence> sequences = new LinkedHashMap<>();
        List<String> repliconsNames = new ArrayList<>();
        String repliconName;
        int i=0;
        while(true) {
            if(max>0 && i>=max) break;
            i++;
            String seqString;
            try{
                seqString = genbankParser.getSequence(bufferedReader, 0);
            }catch (OutOfMemoryError e){
                LOGGER.error("Le fichier '{}' est trop volumineux", file.getName());
                continue;
            }
            repliconName = getRepliconFromLocusOrLocus(genbankParser.getHeader());
            repliconsNames.add(repliconName);
            //reached end of file?
            if(seqString==null){
                if(isEOF(bufferedReader)){
                    break; //Fin de fichier
                }else{
                    continue; // replicon suivant
                }
            }
            DNASequence sequence;
            try{
                AbstractSequence<NucleotideCompound> seq = sequenceCreator.getSequence(seqString, 0);
                if(seq instanceof DNASequence){
                    sequence = (DNASequence) seq;
                }else{
                    LOGGER.warn("La séquence obtenues n'est pas une instance de DNASequence");
                    continue;
                }
            }catch(CompoundNotFoundException e){
                LOGGER.warn("Erreur lors du parsing du fichier '{}', la séquence ADN du replicon '{}' n'est pas valide. {}", file.getName(), repliconName, e.getMessage());
                continue;
            }
            LOGGER.trace("Sequence complète du replicon '{}' : {}", repliconName, seqString);
            genbankParser.getSequenceHeaderParser().parseHeader(genbankParser.getHeader(), sequence);

            // add features to new sequence
            for (String k: genbankParser.getFeatures().keySet()){
                for (AbstractFeature f: genbankParser.getFeatures(k)){
                    //f.getLocations().setSequence(sequence);  // can't set proper sequence source to features. It is actually needed? Don't think so...
                    if("CDS".equals(f.getType())){
                        if(checkCDSFeature(f, seqString.length(), repliconName)){
                            sequence.addFeature(f);
                        }else{
                            LOGGER.trace("CDS du replicon '{}' invalide : {}", repliconName, f.getLocations());
                        }
                    }else{
                        sequence.addFeature(f);
                    }
                }
            }
            if(sequence.getFeaturesByType("CDS").isEmpty()){
                LOGGER.warn("Aucun CDS valide pour le replicon '{}' dans le fichier '{}'", repliconName, file.getName());
            }
            // add taxonomy ID to new sequence
            ArrayList<DBReferenceInfo> dbQualifier = genbankParser.getDatabaseReferences().get("db_xref");
            if (dbQualifier != null){
                DBReferenceInfo q = dbQualifier.get(0);
                sequence.setTaxonomy(new TaxonomyID(q.getDatabase()+":"+q.getId(), DataSource.GENBANK));
            }

            sequences.put(sequence.getAccession().getID(), sequence);
        }

        repliconsNames.removeAll(sequences.keySet()); //Les replicons qui ont une séquence sont valides

        //Tous les replicons traités mais qui ne sont pas valides sont marqués comme parsé
        List<RepliconEntity> replicons = new ArrayList<>(repliconsNames.size());
        for(String replicon : repliconsNames){
            RepliconEntity r = getOrCreateReplicon(replicon);
            if(r != null){
                replicons.add(r);
                r.setParsed(true);
                r.setFileName(null);
            }else{
                LOGGER.warn("Le replicon '{}' n'as pas pu être récupéré", replicon);
            }
        }
        if(!repliconsNames.isEmpty()){
            LOGGER.warn("Les replicons suivants sont invalides dans le fichier '{}' : '{}'", file.getName(), repliconsNames);
        }
        repliconService.saveAll(replicons);

        if (max < 0) {
            close();
        }

        return sequences;
    }

    private String getRepliconFromLocusOrLocus(String locus) {
        Pattern p = Pattern.compile("^.*(NC_\\d+?)([^\\d].*)?$");
        Matcher m = p.matcher(locus);
        if(m.matches()){
            return m.group(1);
        }else{
            return locus;
        }
    }

    private static boolean isEOF(BufferedReader r) throws IOException {
        r.mark(320);
        String line = r.readLine();
        r.reset();
        return line == null;
    }

    public void closeImpl() {
        CommonUtils.closeQuietly(bufferedReader);
    }
}

