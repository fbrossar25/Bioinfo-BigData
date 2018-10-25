package fr.unistra.bioinfo.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(name="TextAreaAppender", category="Core", elementType = "appender", printObject = true)
public class TextAreaAppender extends AbstractAppender {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final StringBuffer buffer = new StringBuffer(1024);

    private static TextArea ta;

    protected TextAreaAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout,ignoreExceptions);
    }

    @Override
    public void append(LogEvent event) {
        if (ta == null) {
            return;
        }
        readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            String s = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = StringUtils.split(buffer.append(s).toString(), System.lineSeparator());
            int numberOfLines = lines.length;
            int overFlowLines = numberOfLines - 200; //200 lignes maximum
            if(overFlowLines > 0){
                buffer.setLength(0);
                for(int i=overFlowLines; i<numberOfLines; i++){
                    buffer.append(lines[i]).append(System.lineSeparator());
                }
                Platform.runLater(() -> ta.setText(buffer.toString()));
            }else{
                Platform.runLater(() -> ta.appendText(s));
            }
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
        }
    }

    public void clear(){
        ta.clear();
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    // the configured attributes.
    @PluginFactory
    public static TextAreaAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TextAreaAppender(name, filter, layout, true);
    }

    public static void setTa(TextArea t){
        ta = t;
    }
}
