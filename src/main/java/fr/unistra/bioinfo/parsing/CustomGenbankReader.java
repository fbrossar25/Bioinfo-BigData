/*
 * Code modifié du FrameWork BioJAVA
 */
package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.DataSource;
import org.biojava.nbio.core.sequence.TaxonomyID;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.features.DBReferenceInfo;
import org.biojava.nbio.core.sequence.io.GenbankSequenceParser;
import org.biojava.nbio.core.sequence.io.template.SequenceCreatorInterface;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;
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
 * Class modifiée pour utiliser l'implémentation modifié de CustomGenbankSequenceParser
 */
public class CustomGenbankReader {

    private static RepliconService repliconService;

    private SequenceCreatorInterface<NucleotideCompound> sequenceCreator;
    private GenbankSequenceParser<DNASequence, NucleotideCompound> genbankParser;
    private BufferedReader bufferedReader;
    private boolean closed;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final File file;
    private final Object synchronizedObject = new Object();

    public boolean isClosed() {
        return closed;
    }

    /**
     * If you are going to use the FileProxyProteinSequenceCreator then you
     * need to use this constructor because we need details about
     * the location of the file.
     * @param file
     * @param headerParser
     * @param sequenceCreator
     * @throws FileNotFoundException if the file does not exist, is a directory
     * 	rather than a regular file, or for some other reason cannot be opened
     * 	for reading.
     * @throws SecurityException if a security manager exists and its checkRead
     * 	method denies read access to the file.
     */
    CustomGenbankReader(
            final File file,
            final SequenceHeaderParserInterface<DNASequence, NucleotideCompound> headerParser,
            final SequenceCreatorInterface<NucleotideCompound> sequenceCreator
    ) throws FileNotFoundException {
        this.file = file;
        this.bufferedReader = new BufferedReader(new FileReader(file));
        this.sequenceCreator = sequenceCreator;
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
     * @throws CompoundNotFoundException
     */
    public LinkedHashMap<String, DNASequence> process() throws IOException {
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
    public LinkedHashMap<String, DNASequence> process(final int max) throws IOException {
        LinkedHashMap<String, DNASequence> sequences = new LinkedHashMap<>();
        List<String> repliconsNames = new ArrayList<>();
        String repliconName;
        int i=0;
        while(true) {
            if(max>0 && i>=max) break;
            i++;
            String seqString = genbankParser.getSequence(bufferedReader, 0);
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
                    logger.warn("La séquence obtenues n'est pas une instance de DNASequence");
                    continue;
                }
            }catch(CompoundNotFoundException e){
                logger.error("Erreur lors du parsing du fichier '{}', la séquence ADN du replicon '{}' n'est pas valide", file.getAbsolutePath(), repliconName, e);
                continue;
            }
            genbankParser.getSequenceHeaderParser().parseHeader(genbankParser.getHeader(), sequence);

            // add features to new sequence
            for (String k: genbankParser.getFeatures().keySet()){
                for (AbstractFeature f: genbankParser.getFeatures(k)){
                    //f.getLocations().setSequence(sequence);  // can't set proper sequence source to features. It is actually needed? Don't think so...
                    sequence.addFeature(f);
                }
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
                r.setDownloaded(true);
            }else{
                logger.warn("Le replicon '{}' n'as pas pu être récupéré", replicon);
            }
        }
        repliconService.saveAll(replicons);

        if (max < 0) {
            close();
        }

        return sequences;
    }

    /**
     * Retourne le replicon du nom donné. Le créé s'il n'existe pas ainsi que le hierarchy associé.
     * @param replicon Le nom du replicon
     * @return Le replicon, où null s'il n'as pas pu être créé
     * @see GenbankParser#createReplicon(File, String)
     */
    private RepliconEntity getOrCreateReplicon(String replicon){
        RepliconEntity entity;
        synchronized(synchronizedObject){
            entity = repliconService.getByName(replicon);
        }
        if(entity == null){
            try {
                return GenbankParser.createReplicon(file, replicon);
            } catch (IOException e) {
                logger.error("Erreur lors de la lecture du fichier", e);
                return null;
            }
        }else{
            return entity;
        }
    }

    private String setParsedFromLocus(String locus){
        String replicon = getRepliconFromLocusOrLocus(locus);
        if(repliconService != null){
            RepliconEntity r = repliconService.getByName(replicon);
            r.setParsed(true);
            repliconService.save(r);
        }
        return replicon;
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

    public void close() {
        try {
            bufferedReader.close();
            this.closed = true;
        } catch (IOException e) {
            logger.error("Couldn't close the reader. {}", e.getMessage());
            this.closed = false;
        }
    }

    public static void setRepliconService(RepliconService repliconService){
        CustomGenbankReader.repliconService = repliconService;
    }
}

