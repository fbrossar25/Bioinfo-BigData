package fr.unistra.bioinfo.gui;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private TextArea textAera;
    private PatternLayoutEncoder encoder;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    private final StringBuffer buffer = new StringBuffer(1024);

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (textAera == null) {
            return;
        }
        readLock.lock();
        String s = new String(encoder.encode(eventObject), StandardCharsets.UTF_8);
        String[] lines = StringUtils.split(buffer.append(s).toString(), System.lineSeparator());
        int numberOfLines = lines.length;
        int overFlowLines = numberOfLines - 200; //200 lignes maximum
        if(overFlowLines > 0){
            buffer.setLength(0);
            for(int i=overFlowLines; i<numberOfLines; i++){
                buffer.append(lines[i]).append(System.lineSeparator());
            }
            Platform.runLater(() -> {
                textAera.setText(buffer.toString());
                textAera.deselect();
                textAera.selectPositionCaret(textAera.getLength());
            });
        }else{
            Platform.runLater(() -> {
                textAera.appendText(s);
                textAera.deselect();
                textAera.selectPositionCaret(textAera.getLength());
            });
        }
        readLock.unlock();
    }

    public void clear(){
        textAera.clear();
    }
    
    public TextArea getTextAera() {
        return textAera;
    }

    public void setTextAera(TextArea textAera) {
        this.textAera = textAera;
        if(textAera != null){
            textAera.setEditable(false);
        }
    }

    public void setEncoder(PatternLayoutEncoder encoder){
        this.encoder = encoder;
    }

    public PatternLayoutEncoder getEncoder(){
        return encoder;
    }
}
