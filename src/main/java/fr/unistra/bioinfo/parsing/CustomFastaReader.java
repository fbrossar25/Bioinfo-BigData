package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.BufferedReaderBytesRead;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.template.SequenceHeaderParserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedHashMap;

/**
 * Use FastaReaderHelper as an example of how to use this class where FastaReaderHelper should be the
 * primary class used to read Fasta files
 * @author Scooter Willis ;lt;willishf at gmail dot com&gt;
 *
 * Classe CustomFasta modifiée
 */
public class CustomFastaReader extends AbstractCustomReader{

    private final static Logger logger = LoggerFactory.getLogger(CustomFastaReader.class);

    private SequenceHeaderParserInterface<DNASequence,NucleotideCompound> headerParser;
    private BufferedReaderBytesRead br;
    private InputStreamReader isr;
    private FileInputStream fi = null;
    private long fileIndex = 0;
    private long sequenceIndex = 0;
    private String line = "";
    private String header= "";

    public CustomFastaReader(File file) throws FileNotFoundException {
        super(file);
        this.headerParser = new GenericFastaHeaderParser<>();
        fi = new FileInputStream(file);
        isr = new InputStreamReader(fi);
        this.br = new BufferedReaderBytesRead(isr);
    }

    LinkedHashMap<String,DNASequence> process() throws IOException {
        return process(-1);
    }

    LinkedHashMap<String,DNASequence> process(int max) throws IOException {
        String line = "";
        if(this.line != null && this.line.length() > 0){
            line=this.line;
        }
        String header = "";
        if(this.header != null && this.header.length() > 0){
            header=this.header;
        }

        StringBuilder sb = new StringBuilder();
        int processedSequences=0;
        boolean keepGoing = true;


        LinkedHashMap<String,DNASequence> sequences = new LinkedHashMap<>();

        do {
            line = line.trim(); // nice to have but probably not needed
            if (line.length() != 0) {
                if (line.startsWith(">")) {//start of new fasta record

                    if (sb.length() > 0) {
                        //i.e. if there is already a sequence before
                        //logger.info("Sequence index=" + sequenceIndex);

                        try {
                            DNASequence sequence = (DNASequence)sequenceCreator.getSequence(sb.toString(), sequenceIndex);
                            headerParser.parseHeader(header, sequence);
                            sequences.put(sequence.getAccession().getID(),sequence);
                            processedSequences++;

                        } catch (CompoundNotFoundException e) {
                            logger.warn("Sequence with header '{}' has unrecognised compounds ({}), it will be ignored",
                                    header, e.getMessage());
                        }

                        sb.setLength(0); //this is faster than allocating new buffers, better memory utilization (same buffer)
                    }
                    header = line.substring(1);
                } else if (line.startsWith(";")) {
                    // ignore comments
                } else {
                    //mark the start of the sequence with the fileIndex before the line was read
                    if(sb.length() == 0){
                        sequenceIndex = fileIndex;
                    }
                    sb.append(line);
                }
            }
            fileIndex = br.getBytesRead();

            line = br.readLine();

            if (line == null) {
                //i.e. EOF
                if ( sb.length() == 0 && header.length() != 0 ) {
                    logger.warn("Can't parse sequence {}. Got sequence of length 0!", sequenceIndex);
                    logger.warn("header: {}", header);
                    header = null;
                } else if ( sb.length() > 0 ) {
                    //logger.info("Sequence index=" + sequenceIndex + " " + fileIndex );
                    try {
                        DNASequence sequence = (DNASequence)sequenceCreator.getSequence(sb.toString(), sequenceIndex);
                        headerParser.parseHeader(header, sequence);
                        sequences.put(sequence.getAccession().getID(),sequence);
                        processedSequences++;
                        header = null;
                    } catch (CompoundNotFoundException e) {
                        logger.warn("Sequence with header '{}' has unrecognised compounds ({}), it will be ignored",
                                header, e.getMessage());
                    }
                }
                keepGoing = false;
            }
            if (max > -1 && processedSequences>=max) {
                keepGoing=false;
            }
        } while (keepGoing);

        this.line  = line;
        this.header= header;
        close();
        return max > -1 && sequences.isEmpty() ? null :  sequences;
    }

    protected void closeImpl() {
        CommonUtils.closeQuietly(br);
        CommonUtils.closeQuietly(isr);
        //If stream was created from File object then we need to close it
        if (fi != null) {
            CommonUtils.closeQuietly(fi);
        }
        this.line=this.header = null;
    }
}
