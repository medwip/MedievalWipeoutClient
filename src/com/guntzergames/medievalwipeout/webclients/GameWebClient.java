package com.guntzergames.medievalwipeout.webclients;

import com.guntzergames.medievalwipeout.activities.GameActivity;
import com.guntzergames.medievalwipeout.interfaces.GameWebClientCallbackable;
import com.guntzergames.medievalwipeout.services.GameCheckerThread;
import com.guntzergames.medievalwipeout.webclients.OnGetGameWebAsyncResponse.ResponseType;
import com.loopj.android.http.AsyncHttpClient;

public class GameWebClient {

	private AsyncHttpClient client = null;
	private OnGetGameWebAsyncResponse onGetGameWebAsyncResponse;
	private GameWebClientCallbackable callbackable = null;
	private String ip = null;
	
	private void get(String url, OnGetGameWebAsyncResponse.ResponseType responseType) {

		onGetGameWebAsyncResponse.setResponseType(responseType);
		
		if ( !(callbackable.isHttpRequestBeingExecuted() && responseType.getPriority() < callbackable.getCurrentRequestPriority()) ) {
			client.get(url,	null, onGetGameWebAsyncResponse);
		}
		else {
			callbackable.onError("Another HTTP request with a highest priority is already begin executed, transaction aborted...");
		}
		
	}

	public GameWebClient(String ip, GameWebClientCallbackable callbackable) {
		client = new AsyncHttpClient();
		this.callbackable = callbackable;
		this.ip = ip;
		onGetGameWebAsyncResponse = new OnGetGameWebAsyncResponse(callbackable);
	}

	public GameWebClientCallbackable getCallbackable() {
		return callbackable;
	}

	public void setCallbackable(GameWebClientCallbackable callbackable) {
		this.callbackable = callbackable;
	}

	public void joinGame(String facebookUserId, long deckId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/join/" + facebookUserId + "/" + deckId, ResponseType.JOIN_GAME);
	}

	public void getGame(long gameId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/get/" + gameId + "/" + callbackable.getFacebookUserId(), ResponseType.GET_GAME);
	}

	public void getGame(long gameId, final GameCheckerThread gameCheckerThread) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/get/" + gameId + "/" + gameCheckerThread.getFacebookUserId(), ResponseType.GET_GAME);
	}

	public void getAccount() {
		get("http://" + ip + ":8080/MedievalWipeout/rest/account/get/" + callbackable.getFacebookUserId(), ResponseType.GET_ACCOUNT);
	}

	public void getOngoingGames() {
		get("http://" + ip + ":8080/MedievalWipeout/rest/account/getGames/" + callbackable.getFacebookUserId(), ResponseType.GET_GAMES);
	}

	public void addDeckTemplateElement(long deckTemplateId, long collectionElementId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/account/addDeckTemplateElement/" + callbackable.getFacebookUserId() + "/" + deckTemplateId + "/" + collectionElementId, ResponseType.GET_ACCOUNT);
	}

	public void openPacket() {
		get("http://" + ip + ":8080/MedievalWipeout/rest/account/openPacket/" + callbackable.getFacebookUserId(), ResponseType.OPEN_PACKET);
	}

	public void nextPhase(long gameId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/nextPhase/" + gameId + "/" + callbackable.getFacebookUserId(), ResponseType.NEXT_PHASE);
	}

	public void playCard(long gameId, String sourceLayout, long sourceCardId, String destinationLayout, long destinationCardId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/play/" + callbackable.getFacebookUserId() + "/" + gameId + "/" + sourceLayout + "/" + sourceCardId + "/" + destinationLayout + "/" + destinationCardId, ResponseType.GET_GAME);
	}
	
	public void playResourceCard(long gameId, long sourceCardId) {
		playCard(gameId, null, sourceCardId, null, -1);
	}

	public void deleteGame(long gameId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/delete/" + gameId, ResponseType.DELETE_GAME);
	}
	
	public void drawInitialHand(long gameId, GameActivity mainActivity) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/drawInitialHand/" + gameId + "/" + mainActivity.getFacebookUserId(), ResponseType.GET_GAME);
	}

	public void getCardModels() {
		get("http://" + ip + ":8080/MedievalWipeout/rest/account/getCardModels", ResponseType.GET_CARD_MODELS);
	}

	public void checkGame(long gameId) {
		get("http://" + ip + ":8080/MedievalWipeout/rest/game/get/" + gameId + "/" + callbackable.getFacebookUserId(), ResponseType.CHECK_GAME);
	}
	
	public void getVersion() {
		get("http://" + ip + ":8080/MedievalWipeout/rest/client/version", ResponseType.GET_VERSION);
	}
	
}
