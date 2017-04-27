package moviemaster;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public class MovieMasterSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

	private static final Set<String> supportedApplicationIds;

    static {
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.477cb321-b587-47ed-a5db-30196e96d3c2");
    }

    public MovieMasterSpeechletRequestStreamHandler() {
        super(new MovieMasterSpeechlet(), supportedApplicationIds);
    }

    public MovieMasterSpeechletRequestStreamHandler(Speechlet speechlet,
            Set<String> supportedApplicationIds) {
        super(speechlet, supportedApplicationIds);
    }
}