package moviemaster;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.*;
import com.amazonaws.util.CollectionUtils;
import com.omertron.omdbapi.OmdbApi;
import com.omertron.omdbapi.model.OmdbVideoFull;
import com.omertron.omdbapi.model.SearchResults;
import com.omertron.omdbapi.tools.OmdbBuilder;

public class MovieMasterSpeechlet implements Speechlet {
	
    private static final Logger log = LoggerFactory.getLogger(MovieMasterSpeechlet.class);

    private static final String SLOT_TITLE_NAME = "name";
    
    private static final String SPEECH_REPROMPT_TEXT =
            "With Movie Master, you can get any information related to a movie."
                + " For example, you could say what do you know about the god father movie, who acted in the god father,"
                + " What's the rating for the god father movie, Who directed the god father, or In which year did the god father movie came out."
                + " Now, What would you like to know?";
    
    private OmdbApi omdb = new OmdbApi();
    
    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("GetMovieInfoIntent".equals(intentName)) {
            return handleQueryByTitleRequest(intent, session);
        } else if ("GetMovieListingsIntent".equals(intentName)) {
        	return handleQueryForListingsRequest(intent, session);
        } else if ("GetMovieRatingIntent".equals(intentName)) {
        	return handleQueryForRatingRequest(intent, session);
        } else if ("GetMovieDirectorIntent".equals(intentName)) {
        	return handleQueryForDirectorRequest(intent, session);
        } else if ("GetMovieActorsIntent".equals(intentName)) {
        	return handleQueryForActorsRequest(intent, session);
        } else if ("GetMoviePlotIntent".equals(intentName)) {
        	return handleQueryForPlotRequest(intent, session);
        } else if ("GetMovieReleaseDateIntent".equals(intentName)) {
        	return handleQueryForReleaseDateRequest(intent, session);
        }else if ("AMAZON.HelpIntent".equals(intentName)) {
            String speechOutput = SPEECH_REPROMPT_TEXT;
            String repromptText = "What information do you need?";
            return newAskResponse(speechOutput, false, repromptText, false);
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");
            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");
            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
    }

    private SpeechletResponse getWelcomeResponse() {
        String speechOutput = "Welcomw to Movie Master. What would you like to know about?";
        String repromptText = SPEECH_REPROMPT_TEXT;
        return newAskResponse(speechOutput, false, repromptText, false);
    }
    
    private SpeechletResponse handleQueryByTitleRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String prefixContent = "For movie " + movieTitle + ", ";
    	setOutputToSpeechAndCard(prefixContent, speechOutputBuilder, cardOutputBuilder);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).build());
    		if(!StringUtils.equalsIgnoreCase(result.getPlot(), "N/A")) {
    			setOutputToSpeechAndCard("Plot for the movie is, "+result.getPlot(), speechOutputBuilder, cardOutputBuilder);
    		}
    		if(!StringUtils.equalsIgnoreCase(result.getActors(), "N/A")) {
    			setOutputToSpeechAndCard(result.getActors()+" acted in this movie.", speechOutputBuilder, cardOutputBuilder);
    		}
    		if(!StringUtils.equalsIgnoreCase(result.getDirector(), "N/A")) {
    			setOutputToSpeechAndCard(result.getDirector()+" directed this movie.", speechOutputBuilder, cardOutputBuilder);
    		}
    		if(!StringUtils.equalsIgnoreCase(result.getImdbRating(), "N/A")) {
    			setOutputToSpeechAndCard("This movie is rated "+ result.getImdbRating()+" on IMDB.", speechOutputBuilder, cardOutputBuilder);
    		}
    		if (cardOutputBuilder.toString().equals(prefixContent+" ")) {
    			setOutputToSpeechAndCard("No movies were found under the title " +movieTitle, speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Information for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
        
        String repromptText = SPEECH_REPROMPT_TEXT;
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForRatingRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String prefixContent = "For movie " + movieTitle + ", ";
    	setOutputToSpeechAndCard(prefixContent, speechOutputBuilder, cardOutputBuilder);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).setTomatoesOn().build());
    		String imdbRating = result.getImdbRating();
    		String rottenTomatoesRating  = result.getTomatoRating();
    		float overAllRating = 0;
    		if (StringUtils.isNotBlank(rottenTomatoesRating) && !StringUtils.equalsIgnoreCase(rottenTomatoesRating, "N/A")) {
    			overAllRating = overAllRating + Float.parseFloat(rottenTomatoesRating);
    			setOutputToSpeechAndCard("Rating on Rotten Tomatoes is " + rottenTomatoesRating, 
    					speechOutputBuilder, cardOutputBuilder);
    		}
    		if (StringUtils.isNotBlank(imdbRating) && !StringUtils.equalsIgnoreCase(imdbRating, "N/A")) {
    			overAllRating = overAllRating + Float.parseFloat(imdbRating);
    			setOutputToSpeechAndCard("IMDB rating is "+ imdbRating, speechOutputBuilder, cardOutputBuilder);
    			if (Float.parseFloat(imdbRating) <= Float.parseFloat("5")) {
        			setOutputToSpeechAndCard("If you are planning on watching this, going to a coffee shop would be a better option.",
        					speechOutputBuilder, cardOutputBuilder);
        		}
    		}
    		if (overAllRating > Float.parseFloat("16")) {
    			setOutputToSpeechAndCard("Well, it sure does look like a good movie.", speechOutputBuilder, cardOutputBuilder);
    		}
    		if ((StringUtils.isBlank(imdbRating) && StringUtils.isBlank(rottenTomatoesRating)) || 
    				StringUtils.equalsIgnoreCase(result.getImdbRating(), "N/A")) {
    			setOutputToSpeechAndCard("No Movie ratings were available for this particular title.", 
    					speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Rating for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;   
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForListingsRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String speechOutput = "";
    	try {
        	SearchResults searchResults = omdb.search(new OmdbBuilder().setSearchTerm(movieTitle).build());
        	String movieListingsText = "Found " +searchResults.getTotalResults() +" movie listings "
        			+ "with title " +movieTitle+ " in it.";
        	setOutputToSpeechAndCard(movieListingsText, speechOutputBuilder, cardOutputBuilder);
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
    	SimpleCard card = new SimpleCard();
        String cardTitle = "Total Listings for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;  
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForActorsRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).build());
    		String actors = result.getActors();
    		if (StringUtils.isNotBlank(actors) && !StringUtils.equalsIgnoreCase(actors, "N/A")) {
    			List<String> actorsList = Arrays.asList(actors.split("\\s*,\\s*"));
    			if(!CollectionUtils.isNullOrEmpty(actorsList)) {
    				List<String> listToExpose = actorsList.stream().limit(5).collect(Collectors.toList()); 
    				for (String actor : listToExpose) {
    					setOutputToSpeechAndCard(actor + ", ", speechOutputBuilder, cardOutputBuilder);
    				}
    				setOutputToSpeechAndCard("are the prominent casting crew in this movie.", 
    						speechOutputBuilder, cardOutputBuilder);			
    			} else {
    				setOutputToSpeechAndCard("There are no actors listed for this particular title.", 
    						speechOutputBuilder, cardOutputBuilder);
    			}
    		} else {
    			setOutputToSpeechAndCard("There are no actors listed for this particular title.", 
						speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Actors in movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;   
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForDirectorRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).build());
    		String director = result.getDirector();
    		if (StringUtils.isNotBlank(director) && !StringUtils.equalsIgnoreCase(director, "N/A")) {
    			setOutputToSpeechAndCard(director+ " directed this movie.", speechOutputBuilder, cardOutputBuilder);
    		} else {
    			setOutputToSpeechAndCard("There are no directors listed for this particular title.", 
    					speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Director for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;   
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForPlotRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).build());
    		String moviePlot = result.getPlot();
    		if (StringUtils.isNotBlank(moviePlot) && !StringUtils.equalsIgnoreCase(moviePlot, "N/A")) {
    			setOutputToSpeechAndCard("Plot for the movie is,  "+ moviePlot, speechOutputBuilder, cardOutputBuilder);
    		} else {
    			setOutputToSpeechAndCard("There is no movie plot listed for this particular title.", 
    					speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Story for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;   
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private SpeechletResponse handleQueryForReleaseDateRequest(Intent intent, Session session) {
    	
    	StringBuilder speechOutputBuilder = new StringBuilder();
    	StringBuilder cardOutputBuilder = new StringBuilder();
    	String movieTitle = getSlotValue(intent, SLOT_TITLE_NAME);
    	String speechOutput = "";
    	try {
    		OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieTitle).build());
    		String dateString = result.getReleased();
    		
    		if (StringUtils.isNotBlank(dateString)) {
    			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.ENGLISH);
        		LocalDate releaseDate = LocalDate.parse(dateString, formatter);
        		if (releaseDate.isAfter(releaseDate)) {
        			setOutputToSpeechAndCard("This movie is supposed to be released on " + releaseDate.toString(), 
        					speechOutputBuilder, cardOutputBuilder);
        			setOutputToSpeechAndCard("Hope it does well!", speechOutputBuilder, cardOutputBuilder);	
        		} else {
        			Period intervalPeriod = Period.between(releaseDate, LocalDate.now());
        			setOutputToSpeechAndCard("This movie was released on " + releaseDate, 
        					speechOutputBuilder, cardOutputBuilder);
        			if (intervalPeriod.getYears() > 30) {
        				setOutputToSpeechAndCard("Good lord! This movie is really old.", speechOutputBuilder, cardOutputBuilder);
        				setOutputToSpeechAndCard("It's been already "+intervalPeriod.getYears() +"years.", 
        						speechOutputBuilder, cardOutputBuilder);
        			}
        		}
    		} else {
    			setOutputToSpeechAndCard("Release date for the movie is not available.", speechOutputBuilder, cardOutputBuilder);
    		}
    	} catch(Exception e) {
    		speechOutput = "<p> No movies were found under the title " +movieTitle+ ".Try again with a different name </p>";
            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
            outputSpeech.setSsml("<speak>" + speechOutput + "</speak>");
            return SpeechletResponse.newTellResponse(outputSpeech);
    	}
    	speechOutput = speechOutputBuilder.toString();
        SimpleCard card = new SimpleCard();
        String cardTitle = "Release date information for movie " + movieTitle;
        card.setTitle(cardTitle);
        card.setContent(cardOutputBuilder.toString());
    	
        String repromptText = SPEECH_REPROMPT_TEXT;   
        SpeechletResponse response = newAskResponse("<speak>" + speechOutput + "</speak>", true, repromptText, false);
        response.setCard(card);
        return response;	
    }
    
    private void setOutputToSpeechAndCard(String message, StringBuilder speechBuilder, StringBuilder cardBuilder) {
    	speechBuilder.append("<p>" + message + "</p> ");
    	cardBuilder.append(message + " ");
    }
    
    private String getSlotValue(Intent intent, String slotType) {
        Slot slot = intent.getSlot(slotType);
        if (slot != null && slot.getValue() != null) {
        	return slot.getValue().replaceAll("[.]","");
        } else {
        	log.error("slot value for intent is empty");
            return new String("");
        }
    }
    
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
            String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }
        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}