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

    private TextArea textArea;
    private PatternLayoutEncoder encoder;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    private final StringBuffer buffer = new StringBuffer(1024);

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (textArea == null) {
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
                double scroll = textArea.getScrollTop();
                textArea.setText(buffer.toString());
                textArea.setScrollTop(scroll);
            });
        }else{
            Platform.runLater(() -> {
                double scroll = textArea.getScrollTop();
                textArea.appendText(s);
                textArea.setScrollTop(scroll);
            });
        }
        readLock.unlock();
    }

    private void appendLater(final String s){

    }

    public void clear(){
        buffer.setLength(0);
        textArea.clear();
    }
    
    public TextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
        if(textArea != null){
            textArea.setEditable(false);
        }
    }

    public void setEncoder(PatternLayoutEncoder encoder){
        this.encoder = encoder;
    }

    public PatternLayoutEncoder getEncoder(){
        return encoder;
    }
}
