package com.guntzergames.medievalwipeout.webclients;

import org.apache.http.Header;

import android.util.Log;

import com.guntzergames.medievalwipeout.beans.Account;
import com.guntzergames.medievalwipeout.beans.CardModelList;
import com.guntzergames.medievalwipeout.beans.DiffResult;
import com.guntzergames.medievalwipeout.beans.GameViewList;
import com.guntzergames.medievalwipeout.beans.Packet;
import com.guntzergames.medievalwipeout.exceptions.JsonException;
import com.guntzergames.medievalwipeout.interfaces.GameWebClientCallbackable;
import com.guntzergames.medievalwipeout.utils.DiffUtils;
import com.guntzergames.medievalwipeout.views.GameView;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class OnGetGameWebAsyncResponse extends AsyncHttpResponseHandler {

	private static final String TAG = "OnGetGameWebAsyncResponse";

	private GameWebClientCallbackable callbackable;
	private ResponseType responseType;

	private String previousJsonResponse;

	public enum ResponseType {

		GET_GAME(0), CHECK_GAME(0), NEXT_PHASE(1), JOIN_GAME(1), DELETE_GAME(1), GIVE_UP_GAME(1), GET_ACCOUNT(1), GET_CARD_MODELS(1), GET_GAMES(1), OPEN_PACKET(1), GET_VERSION(1), GET_PACKAGE(
				1);

		private int priority;

		private ResponseType(int priotity) {
			this.priority = priotity;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

	};

	public OnGetGameWebAsyncResponse(GameWebClientCallbackable callbackable) {
		this.callbackable = callbackable;
	}

	public GameWebClientCallbackable getCallbackable() {
		return callbackable;
	}

	public void setCallbackable(GameWebClientCallbackable callbackable) {
		this.callbackable = callbackable;
	}

	public ResponseType getResponseType() {
		return responseType;
	}

	public void setResponseType(ResponseType responseType) {
		this.responseType = responseType;
	}
	
	public boolean getFullJson() {
		return (previousJsonResponse == null ? true : false);
	}

	@Override
	public void onStart() {
		super.onStart();
		callbackable.setHttpRequestBeingExecuted(true);
		callbackable.setCurrentRequestPriority(responseType.getPriority());
	}

	@Override
	public void onFinish() {
		super.onFinish();
		callbackable.setHttpRequestBeingExecuted(false);
	}

	@Override
	public void onSuccess(String response) {

		Log.i(TAG, String.format("Perf Monitor: Successfull call to %s by %s [%s]", responseType, callbackable.getClass().getSimpleName(), callbackable.getHttpCallsDone()));

		try {

			switch (responseType) {
				case GET_ACCOUNT:
					callbackable.onGetAccount(Account.fromJson(response));
					break;
				case GET_GAME:
				case CHECK_GAME:
					
					String json = null;
					boolean reload = true;
					
					if (previousJsonResponse != null) {

						Log.i(TAG, "Previous response not null");
						if (response.equals("")) {
							json = previousJsonResponse;
							Log.i(TAG, String.format("Response blank, json=%s", json.substring(0, 1000)));
							reload = false;
						} else {
							
							Log.i(TAG, String.format("Response not blank: %s", response.substring(0, 1000)));
							
							try {
								DiffResult diff = DiffResult.fromJson(response);
								json = DiffUtils.patch(previousJsonResponse, diff);
								Log.i(TAG, "After patch, JSON: " + json.substring(0, 1000));
							}
							catch ( JsonException e ) {
								json = response;
							}
							
						}

					} else {
						Log.i(TAG, "Previous response is null");
						json = response;
					}
//					Log.i(TAG, "JSON: " + json);
					GameView gameView = GameView.fromJson(json);
					previousJsonResponse = json;
					
					if ( reload ) {
						
						if (responseType == ResponseType.CHECK_GAME) {
							callbackable.onCheckGame(gameView);
						} else {
							callbackable.onGetGame(gameView);
						}
						
					}
					
					break;
				case NEXT_PHASE:
					callbackable.onGetGame(GameView.fromJson(response));
					break;
				case JOIN_GAME:
					callbackable.onGameJoined(GameView.fromJson(response));
					break;
				case DELETE_GAME:
					callbackable.onDeleteGame();
					break;
				case GET_CARD_MODELS:
					callbackable.onGetCardModels(CardModelList.fromJson(response).getCardModels());
					break;
				case GET_GAMES:
					callbackable.onGetGames(GameViewList.fromJson(response).getGameViews());
					break;
				case OPEN_PACKET:
					callbackable.onOpenPacket(Packet.fromJson(response));
					break;
				case GET_VERSION:
					callbackable.onGetVersion(response);
					break;
				default:
					break;

			}

		} catch (Exception e) {

			callbackable.onError(String.format("Error occured for event %s: %s", responseType, e.getMessage()));
			Log.e(TAG, e.getMessage(), e);

		}

	}

	@Override
	public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
		callbackable.onError(responseType + " " + callbackable.getClass() + " " + callbackable.isInterruptedSignalSent() + " " + arg3.getMessage());
		if (!callbackable.isInterruptedSignalSent()) {
			// callbackable.onError(callbackable.isInterruptedSignalSent() + " "
			// + arg3.getMessage());
		}
	}

}
