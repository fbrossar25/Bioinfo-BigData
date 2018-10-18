package fr.unistra.bioinfo.gui;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.scene.control.TextArea;

import java.nio.charset.StandardCharsets;

public class TextAreaAppender extends AppenderBase<ILoggingEvent> {

    private TextArea textAera;
    private PatternLayoutEncoder encoder;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(textAera != null){
            textAera.appendText(new String(encoder.encode(eventObject), StandardCharsets.UTF_8));
        }
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
