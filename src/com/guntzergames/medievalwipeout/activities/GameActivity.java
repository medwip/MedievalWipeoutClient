package com.guntzergames.medievalwipeout.activities;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.guntzergames.medievalwipeout.abstracts.AbstractCard;
import com.guntzergames.medievalwipeout.abstracts.AbstractCardList;
import com.guntzergames.medievalwipeout.beans.GameEvent;
import com.guntzergames.medievalwipeout.beans.GameEvent.PlayerType;
import com.guntzergames.medievalwipeout.beans.GameEventDeckCard;
import com.guntzergames.medievalwipeout.beans.GameEventPlayCard;
import com.guntzergames.medievalwipeout.beans.GameEventPlayCard.EventType;
import com.guntzergames.medievalwipeout.beans.GameEventResourceCard;
import com.guntzergames.medievalwipeout.beans.Player;
import com.guntzergames.medievalwipeout.beans.PlayerDeckCard;
import com.guntzergames.medievalwipeout.beans.PlayerFieldCard;
import com.guntzergames.medievalwipeout.beans.PlayerFieldCard.Location;
import com.guntzergames.medievalwipeout.beans.PlayerHandCard;
import com.guntzergames.medievalwipeout.beans.ResourceDeckCard;
import com.guntzergames.medievalwipeout.enums.CardLocation;
import com.guntzergames.medievalwipeout.enums.Phase;
import com.guntzergames.medievalwipeout.interfaces.ClientConstants;
import com.guntzergames.medievalwipeout.interfaces.ICard;
import com.guntzergames.medievalwipeout.layouts.CardLayout;
import com.guntzergames.medievalwipeout.layouts.PlayerLayout;
import com.guntzergames.medievalwipeout.listeners.GameAnimationListener;
import com.guntzergames.medievalwipeout.listeners.GameDragListener;
import com.guntzergames.medievalwipeout.listeners.GameResourceListener;
import com.guntzergames.medievalwipeout.listeners.HighlightAnimationListener;
import com.guntzergames.medievalwipeout.listeners.PlayerChoiceListener;
import com.guntzergames.medievalwipeout.services.GameCheckerThread;
import com.guntzergames.medievalwipeout.views.GameView;
import com.guntzergames.medievalwipeout.webclients.GameWebClient;

public class GameActivity extends ApplicationActivity {

	private static final String TAG = "GameActivity";

	private RelativeLayout layout = null, playerChoicesLayout;
	private long gameId;
	private String gameCommand;
	private Player player, opponent;
	private String facebookUserId;
	private GameCheckerThread gameCheckerThread;
	private boolean beingModified = false, opponentDefenseDown = false;
	private CardLayout cardLayoutDetail = null;
	private PlayerLayout playerPlayerLayout, opponentPlayerLayout;

	private CardDetailListener cardDetailListener;
	private GameResourceListener gameResourceListener;
	private PlayerChoiceListener playerResourceListener;
	private GameDragListener gameDragListener;

	private GameAnimationListener gameAnimationListener;
	private HighlightAnimationListener highlightAnimationListener;
	private AnimationSet cardEventAnimationSet, highlightAnimationSet;
	private TranslateAnimation cardEventTranslationAnimation, highlightTranslationAnimation;
	private AlphaAnimation highlightAlphaAnimation;

	private LinearLayout playerHandLayout, opponentFieldDefenseLayout, opponentFieldAttackLayout, playerFieldDefenseLayout, playerFieldAttackLayout, highlightLayout;
	private GridLayout gameResourcesLayout;
	private int touchEvents = 0;
	private CardLayout playerChoiceCard1Layout, playerChoiceCard2Layout, gameEventLayout;

	private HorizontalScrollView handScrollView;

	private TextView gameInfos, gameTrade, gameDefense, gameFaith;

	private Set<View> dragableRegisteredViews = new HashSet<View>();

	private Button nextPhaseButton;

	final private Handler checkGameHandler = new Handler() {

		@Override
		public void handleMessage(Message message) {
			super.handleMessage(message);
			GameView game = (GameView) message.obj;
			if (!beingModified) {
				onGetGame(game);
			} else {
				++httpCallsAborted;
			}
		}
	};

	public long getGameId() {
		return gameId;
	}

	public boolean isBeingModified() {
		return beingModified;
	}

	public void setBeingModified(boolean beingModified) {
		this.beingModified = beingModified;
		ImageView beingModifiedImageView = (ImageView) layout.findViewById(R.id.beingModified);
		beingModifiedImageView.setImageDrawable(getResources().getDrawable(beingModified ? R.drawable.red : R.drawable.green));
	}

	private class CardDetailListener implements OnTouchListener {

		private static final String TAG = "CardDetailListener";

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			setBeingModified(true);

			CardLayout cardLayout = ((CardLayout) v);
			ICard card = cardLayout.getCard();

			Log.d(TAG, String.format("onTouch event detected: %s for event %s", event.getAction(), v.getClass().getName()));

			if (event.getAction() == MotionEvent.ACTION_DOWN) {

				cardLayoutDetail.setDetailShown(true);
				cardLayoutDetail.setup(getMainActivity(), card, cardLayout.getSeqNum(), cardLayout.getCardLocation());
				cardLayoutDetail.show();
				// v.setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
				Log.i(TAG, String.format("Showing %s, touchEvents: %s", card, ++touchEvents));

				v.performClick();

				if (gameView.isActivePlayer()) {

					if ((cardLayout.getCard() instanceof ResourceDeckCard) && gameView.getPhase() == getDuringPlayerResourceChoosePhase()) {
						Log.i(TAG, "Drag initiated during player resource draw");
						ClipData data = ClipData.newPlainText("", "");
						DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(cardLayout);
						cardLayout.startDrag(data, shadowBuilder, cardLayout, 0);
					}

					if ((cardLayout.getCard() instanceof PlayerDeckCard) && gameView.getPhase() == getDuringPlayerDeckDrawPhase()) {
						Log.i(TAG, "Drag initiated during player deck draw");
						ClipData data = ClipData.newPlainText("", "");
						DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(cardLayout);
						cardLayout.startDrag(data, shadowBuilder, cardLayout, 0);
					}

					if ((cardLayout.getCard() instanceof PlayerHandCard) && gameView.getPhase() == getDuringPlayerPlayPhase() && ((PlayerHandCard) card).isPlayable()) {
						Log.i(TAG, "Drag initiated during player play from hand");
						ClipData data = ClipData.newPlainText("", "");
						DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
						v.startDrag(data, shadowBuilder, v, 0);
					}

					if ((cardLayout.getCard() instanceof PlayerFieldCard) && gameView.getPhase() == getDuringPlayerPlayPhase() && !((PlayerFieldCard) card).isPlayed()) {
						Log.i(TAG, "Drag initiated during player play from field");
						ClipData data = ClipData.newPlainText("", "");
						DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
						v.startDrag(data, shadowBuilder, v, 0);
					}

				}

				return true;

			}

			if (event.getAction() == MotionEvent.ACTION_UP) {

				hideCardLayoutDetail();
				// v.setBackgroundColor(getResources().getColor(R.color.red));
				Log.d(TAG, "Hiding " + card);
				setBeingModified(false);
				return true;

			}

			setBeingModified(false);
			return false;

		}

	}

	private Phase getDuringPlayerPlayPhase() {

		return Phase.DURING_PLAY;

	}

	private Phase getDuringPlayerResourceChoosePhase() {

		return Phase.DURING_RESOURCE_CHOOSE;

	}

	private Phase getDuringPlayerResourceSelectPhase() {

		return Phase.DURING_RESOURCE_SELECT;

	}

	private Phase getDuringPlayerDeckDrawPhase() {

		return Phase.DURING_DECK_DRAW;

	}

	protected GameActivity getMainActivity() {
		return this;
	}

	public Set<View> getDragableRegisteredViews() {
		return dragableRegisteredViews;
	}

	public void setDragableRegisteredViews(Set<View> dragableRegisteredViews) {
		this.dragableRegisteredViews = dragableRegisteredViews;
	}

	public void hideCardLayoutDetail() {
		cardLayoutDetail.hide();
	}

	private void initField(LinearLayout layout) {

		for (int i = 0; i < layout.getChildCount(); i++) {

			CardLayout cardInFieldLayout = (CardLayout) layout.getChildAt(i);
			cardInFieldLayout.setOnTouchListener(cardDetailListener);
			cardInFieldLayout.hide();

		}

	}

	private void initOpponentDragListener() {

		if (opponentDefenseDown) {
			unregisterDragListener(opponentFieldDefenseLayout);
		} else {
			registerDragListener(opponentFieldDefenseLayout);
		}

		int len = opponent.getPlayerFieldAttack().getCards().size();

		for (int i = 0; i < Math.min(opponentFieldAttackLayout.getChildCount(), len); i++) {

			CardLayout cardInFieldLayout = (CardLayout) opponentFieldAttackLayout.getChildAt(i);
			registerDragListener(cardInFieldLayout);

		}

		if (opponentDefenseDown) {
			len = opponent.getPlayerFieldDefense().getCards().size();

			for (int i = 0; i < Math.min(opponentFieldDefenseLayout.getChildCount(), len); i++) {

				CardLayout cardInFieldLayout = (CardLayout) opponentFieldDefenseLayout.getChildAt(i);
				registerDragListener(cardInFieldLayout);

			}
		}

	}

	private void setupField(AbstractCardList<?> cardList, String layoutPrefix, int numElem, CardLocation cardLocation) {

		int num = cardList.getCards().size();
		for (int i = 0; i < numElem; i++) {

			CardLayout cardLayout = (CardLayout) layout.findViewById(CardLayout.getCardFromId(layoutPrefix, i));
			if (i < num) {
				AbstractCard card = cardList.getCards().get(i);
				cardLayout.setup(this, card, i, cardLocation);
				cardLayout.show();
				Log.d(TAG, "Set view " + cardLayout + " visible " + cardLayout.getVisibility());
			} else {
				cardLayout.hide();
			}
		}

	}

	private void initHighlightLayout() {

		highlightAnimationSet = new AnimationSet(false);
		highlightAnimationSet.setDuration(1500);

		highlightTranslationAnimation = new TranslateAnimation(0, 0, 0, 0);
		highlightTranslationAnimation.setFillAfter(true);
		highlightTranslationAnimation.setFillEnabled(true);
		highlightAnimationSet.addAnimation(highlightTranslationAnimation);

		highlightAlphaAnimation = new AlphaAnimation(0f, .8f);
		highlightAnimationSet.addAnimation(highlightAlphaAnimation);

		highlightAnimationSet.setAnimationListener(highlightAnimationListener);

	}

	private AnimationDrawable getAnimationDrawable(View view, int index) {

		Drawable background = view.getBackground();

		if (background instanceof LayerDrawable) {

			LayerDrawable layerDrawable = (LayerDrawable) background;
			int num = 0;

			for (int i = 0; i < layerDrawable.getNumberOfLayers(); i++) {

				Drawable drawable = layerDrawable.getDrawable(i);
				if (drawable instanceof AnimationDrawable) {
					AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
					if (num++ == index) {
						return animationDrawable;
					}
				}

			}

		}

		return null;

	}

	public void startAnimation(View view, int index) {

		AnimationDrawable animationDrawable = getAnimationDrawable(view, index);
		if (animationDrawable != null) {
			animationDrawable.setAlpha(255);
			animationDrawable.start();
		}

	}

	public void stopAnimation(View view, int index) {

		AnimationDrawable animationDrawable = getAnimationDrawable(view, index);
		if (animationDrawable != null) {
			animationDrawable.setAlpha(0);
			animationDrawable.stop();
		}

	}

	public void startHighlightAnimation(View view) {

		startAnimation(view, 0);

	}

	public void stopHightlightAnimation(View view) {

		stopAnimation(view, 0);

	}

	public void startTargetAnimation(View view) {

		startAnimation(view, 1);

	}

	public void startTargetAnimationLayout(LinearLayout layout) {

		for (int i = 0; i < layout.getChildCount(); i++) {
			startTargetAnimation(layout.getChildAt(i));
		}

	}

	public void stopTargetAnimationLayout(ViewGroup layout) {

		for (int i = 0; i < layout.getChildCount(); i++) {
			stopTargetAnimation(layout.getChildAt(i));
		}

	}

	public void stopTargetAnimation(View view) {

		stopAnimation(view, 1);

	}

	private void registerDragListener(View view) {

		view.setOnDragListener(gameDragListener);
		dragableRegisteredViews.add(view);

	}

	private void unregisterDragListener(View view) {

		view.setOnDragListener(null);
		dragableRegisteredViews.remove(view);

	}

	public void updateCardsDisplay() {

		Log.i(TAG, String.format("Entering updateCardsDisplay() [%s], phase=%s", httpCallsDone, gameView.getPhase()));

		if (gameView.isActivePlayer()) {
			player.updatePlayableHandCards();
		}

		setupField(opponent.getPlayerFieldDefense(), "opponentFieldDefense", 5, CardLocation.FIELD_DEFENSE);
		setupField(opponent.getPlayerFieldAttack(), "opponentFieldAttack", 5, CardLocation.FIELD_ATTACK);
		setupField(player.getPlayerFieldDefense(), "playerFieldDefense", 5, CardLocation.FIELD_DEFENSE);
		setupField(player.getPlayerFieldAttack(), "playerFieldAttack", 5, CardLocation.FIELD_ATTACK);
		setupField(player.getPlayerHand(), "playerHand", 10, CardLocation.HAND);

		playerChoiceCard1Layout.hide();
		playerChoiceCard2Layout.hide();
		playerChoicesLayout.setVisibility(View.INVISIBLE);

		stopHightlightAnimation(playerChoiceCard1Layout);
		stopHightlightAnimation(playerChoiceCard2Layout);
		stopHightlightAnimation(playerHandLayout);
		stopTargetAnimationLayout(playerHandLayout);
		stopTargetAnimationLayout(playerFieldDefenseLayout);
		stopTargetAnimationLayout(playerFieldAttackLayout);
		stopTargetAnimationLayout(opponentFieldDefenseLayout);
		stopTargetAnimationLayout(opponentFieldAttackLayout);
		stopTargetAnimationLayout(playerChoicesLayout);
		stopTargetAnimation(cardLayoutDetail);
		stopHightlightAnimation(gameResourcesLayout);
		stopHightlightAnimation(playerChoicesLayout);

		// stopTargetAnimation(playerFieldDefenseLayout);

		Phase phase = gameView.getPhase();

		switch (phase) {
			case DURING_RESOURCE_CHOOSE:
				displayPlayerChoices(gameView.getResourceCard1(), gameView.getResourceCard2());
				break;
			case DURING_DECK_DRAW:
				displayPlayerChoices(player.getPlayerDeckCard1(), player.getPlayerDeckCard2());
				break;
			case DURING_RESOURCE_SELECT:
				startHighlightAnimation(gameResourcesLayout);
				break;
			case DURING_PLAY:
				initOpponentDragListener();
				break;
			default:
				onError(String.format("Unsupported phase: %s", phase));
				break;
		}

		Log.i(TAG, String.format("Leaving updateCardsDisplay() [%s]", httpCallsDone));

	}

	private void displayPlayerChoices(ICard cardLayout1, ICard cardLayout2) {

		playerChoiceCard1Layout.setup(this, cardLayout1, 1, CardLocation.MODAL);
		playerChoiceCard2Layout.setup(this, cardLayout2, 2, CardLocation.MODAL);

		if (gameView.isActivePlayer()) {
			startHighlightAnimation(playerChoiceCard1Layout);
			startHighlightAnimation(playerChoiceCard2Layout);
		}

		playerChoiceCard1Layout.show();
		playerChoiceCard2Layout.show();
		playerChoicesLayout.setVisibility(View.VISIBLE);

	}

	public GameWebClient getGameWebClient() {
		return gameWebClient;
	}

	@Override
	protected void onResume() {

		super.onResume();

		initIntentExtras();
		Log.i(TAG, String.format("onResume, gameId=%s", gameId));
		gameCheckerThread.setPaused(false);
		gameCheckerThread.getGame(gameId);

	}

	@Override
	protected void onNewIntent(Intent intent) {

		super.onNewIntent(intent);
		Log.i(TAG, String.format("onNewIntent, gameId=%s", gameId));

	}

	@Override
	protected void onPause() {

		super.onPause();

		Log.i(TAG, "onPause");
		gameCheckerThread.setPaused(true);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");

		init();

		cardLayoutDetail = (CardLayout) layout.findViewById(R.id.card_layout_detail);
		cardLayoutDetail.setDetailShown(true);
		hideCardLayoutDetail();
		gameEventLayout = (CardLayout) layout.findViewById(R.id.gameEvent);
		gameEventLayout.setVisibility(View.INVISIBLE);
		opponentPlayerLayout = (PlayerLayout) layout.findViewById(R.id.opponentPlayerLayout);
		playerPlayerLayout = (PlayerLayout) layout.findViewById(R.id.playerPlayerLayout);

		playerHandLayout = (LinearLayout) layout.findViewById(R.id.playerHand);
		playerFieldDefenseLayout = (LinearLayout) layout.findViewById(R.id.playerFieldDefense);
		playerFieldAttackLayout = (LinearLayout) layout.findViewById(R.id.playerFieldAttack);
		opponentFieldDefenseLayout = (LinearLayout) layout.findViewById(R.id.opponentFieldDefense);
		opponentFieldAttackLayout = (LinearLayout) layout.findViewById(R.id.opponentFieldAttack);
		playerChoicesLayout = (RelativeLayout) layout.findViewById(R.id.playerChoices);
		gameResourcesLayout = (GridLayout) layout.findViewById(R.id.gameResources);

		gameTrade = (TextView) layout.findViewById(R.id.gameTrade);
		gameDefense = (TextView) layout.findViewById(R.id.gameDefense);
		gameFaith = (TextView) layout.findViewById(R.id.gameFaith);

		cardDetailListener = new CardDetailListener();
		gameResourceListener = new GameResourceListener(this);
		playerResourceListener = new PlayerChoiceListener(this);
		gameDragListener = new GameDragListener(this);

		gameAnimationListener = new GameAnimationListener(this);
		highlightAnimationListener = new HighlightAnimationListener(highlightLayout);

		handScrollView = (HorizontalScrollView) layout.findViewById(R.id.handScrollView);

		initField(playerHandLayout);
		initField(opponentFieldDefenseLayout);
		initField(opponentFieldAttackLayout);
		initField(playerFieldDefenseLayout);
		initField(playerFieldAttackLayout);

		// TODO: Verify if this is still needed
		initHighlightLayout();
		initCardEventAnimation();

		Button stopGameButton = (Button) layout.findViewById(R.id.stopGameButton);
		stopGameButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Clicked on delete for game " + gameId);
				gameCheckerThread.setInterruptedSignalSent(true);
				gameWebClient.deleteGame(gameId);
			}
		});

		Button homeButton = (Button) layout.findViewById(R.id.homeButton);
		homeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				returnHome(ClientConstants.GAME_IN_PROGRESS);
			}
		});

		Button getGameButton = (Button) layout.findViewById(R.id.getGameButton);
		getGameButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cardLayoutDetail.show();
				setBeingModified(false);
				getGame(gameId);
			}
		});

		nextPhaseButton = (Button) layout.findViewById(R.id.nextPhaseButton);
		nextPhaseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				nextPhase(gameId);
			}
		});

		gameTrade.setOnClickListener(gameResourceListener);
		gameDefense.setOnClickListener(gameResourceListener);
		gameFaith.setOnClickListener(gameResourceListener);

		registerDragListener(layout);
		registerDragListener(playerHandLayout);
		registerDragListener(playerFieldDefenseLayout);
		playerFieldDefenseLayout.setOnDragListener(gameDragListener);
		registerDragListener(playerFieldAttackLayout);
		registerDragListener(opponentFieldDefenseLayout);
		registerDragListener(opponentFieldAttackLayout);

		playerChoiceCard1Layout = (CardLayout) layout.findViewById(CardLayout.getCardFromId("playerChoice", 0));
		playerChoiceCard1Layout.setOnClickListener(playerResourceListener);
		playerChoiceCard1Layout.setDetailShown(true);
		playerChoiceCard2Layout = (CardLayout) layout.findViewById(CardLayout.getCardFromId("playerChoice", 1));
		playerChoiceCard2Layout.setOnClickListener(playerResourceListener);
		playerChoiceCard2Layout.setDetailShown(true);

		gameCheckerThread = new GameCheckerThread(checkGameHandler, gameId, this);
		gameCheckerThread.start();
		hideCardLayoutDetail();

		handScrollView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				if (event.getAction() == MotionEvent.ACTION_UP) {
					hideCardLayoutDetail();
				}
				v.performClick();

				return false;
			}
		});

	}

	@Override
	protected void onStop() {
		super.onStop();
		gameCheckerThread.setInterruptedSignalSent(true);
	}

	private void initIntentExtras() {

		gameId = getIntent().getExtras().getLong(ClientConstants.GAME_ID);
		gameCommand = getIntent().getExtras().getString(ClientConstants.GAME_COMMAND);
		facebookUserId = getIntent().getExtras().getString(ClientConstants.FACEBOOK_USER_ID);

		Log.i(TAG, String.format("Command %s for game ID: %s, facebookUserId=%s", gameCommand, gameId, facebookUserId));

	}

	private void init() {

		initIntentExtras();

		layout = (RelativeLayout) LinearLayout.inflate(this, R.layout.activity_game, null);

		setContentView(layout);

		getGame(gameId);

	}

	public void returnHome(int status) {
		/*
		 * Intent result = new Intent(); setResult(RESULT_OK, result); finish();
		 */

		setInterruptedSignalSent(true);
		Intent intent = new Intent(GameActivity.this, HomeActivity.class);
		intent.putExtra(ClientConstants.GAME_ID, gameId);
		intent.putExtra(ClientConstants.GAME_STATE, status);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		gameCheckerThread.setInterruptedSignalSent(true);
		finish();
	}

	public String getFacebookUserId() {
		return facebookUserId;
	}

	public void setFacebookUserId(String facebookUserId) {
		this.facebookUserId = facebookUserId;
	}

	public void getGame(long gameId) {
		Log.i(TAG, "Enterring getGame()");
		gameWebClient.getGame(gameId);
		Log.i(TAG, "Leaving getGame()");
	}

	public void nextPhase(long gameId) {
		gameWebClient.nextPhase(gameId);
	}

	private boolean displayEvents() {

		Log.i(TAG, "Enterring displayEvents()");

		boolean handleUpdateDisplay = false;
		LinkedList<GameEvent> events = player.getEvents();
		String str = "Events... ";

		if (events != null && events.size() > 0) {

			for (GameEvent event : events) {

				if (event instanceof GameEventPlayCard) {

					GameEventPlayCard gameEventPlayCard = (GameEventPlayCard) event;
					PlayerDeckCard source = gameEventPlayCard.getSource();
					PlayerDeckCard destination = gameEventPlayCard.getDestination();
					PlayerType type = gameEventPlayCard.getPlayerType();

					Log.i(TAG, String.format("Display event: %s, type=%s, source=%s, destination=%s", gameEventPlayCard, type, source, destination));

					switch (gameView.getPhase()) {

						case DURING_PLAY:

							if (type == PlayerType.PLAYER && source instanceof PlayerHandCard && destination instanceof PlayerFieldCard) {

								CardLayout sourceCardLayout = (CardLayout) layout.findViewById(CardLayout.getCardFromId("playerHand", gameEventPlayCard.getSourceIndex()));
								handleUpdateDisplay = true;
								animateCardEvent(sourceCardLayout.getCard(), playerHandLayout, playerFieldDefenseLayout);

							} else if (type == PlayerType.PLAYER && source instanceof PlayerFieldCard && destination instanceof PlayerFieldCard) {

								PlayerFieldCard playerFieldCardSource = (PlayerFieldCard) source;
								CardLayout sourceCardLayout = (CardLayout) layout.findViewById(CardLayout.getCardFromId(playerFieldCardSource.getField(),
										gameEventPlayCard.getSourceIndex()));
								PlayerFieldCard playerFieldCardDestination = (PlayerFieldCard) destination;
								CardLayout destinationCardLayout = (CardLayout) layout.findViewById(CardLayout.getCardFromId(playerFieldCardDestination.getField(),
										gameEventPlayCard.getDestinationIndex()));
								Log.i(TAG, String.format("playerFieldCard=%s, location=%s, playerFieldCard.getField()=%s", playerFieldCardSource,
										playerFieldCardSource.getLocation(), playerFieldCardSource.getField()));
								handleUpdateDisplay = true;

								View destinationView = destinationCardLayout;

								if (gameEventPlayCard.getEventType() == EventType.ATTACK_DEFENSE_FIELD) {
									destinationView = opponentFieldDefenseLayout;
								} else if (gameEventPlayCard.getEventType() == EventType.ATTACK_ATTACK_CARD) {
									destinationView = (CardLayout) layout.findViewById(CardLayout.getCardFromId("opponentFieldAttack", gameEventPlayCard.getDestinationIndex()));
								}

								animateCardEvent(sourceCardLayout.getCard(), (View) sourceCardLayout.getParent(), destinationView);

							} else if (type == PlayerType.OPPONENT && source instanceof PlayerHandCard && destination instanceof PlayerFieldCard) {

								handleUpdateDisplay = true;
								PlayerFieldCard playerFieldCardDestination = (PlayerFieldCard) destination;
								animateCardEvent(destination, gameInfos, playerFieldCardDestination.getLocation().equals(Location.DEFENSE) ? opponentFieldDefenseLayout
										: opponentFieldAttackLayout);

							} else if (type == PlayerType.OPPONENT && source instanceof PlayerFieldCard && destination instanceof PlayerFieldCard) {

								handleUpdateDisplay = true;
								PlayerFieldCard playerFieldCardSource = (PlayerFieldCard) source;
								PlayerFieldCard playerFieldCardDestination = (PlayerFieldCard) destination;
								animateCardEvent(destination,
										playerFieldCardSource.getLocation().equals(Location.DEFENSE) ? opponentFieldDefenseLayout : opponentFieldAttackLayout,
										playerFieldCardDestination.getLocation().equals(Location.DEFENSE) ? playerFieldDefenseLayout : playerFieldAttackLayout);

							} else {
								str += gameEventPlayCard.toString() + "\n";
							}

							break;

						default:
							onError(String.format("Unhandled phase in displayEvent: %s", gameView.getPhase()));

					}

				}

				else if (event instanceof GameEventResourceCard) {

					GameEventResourceCard gameEventResourceCard = (GameEventResourceCard) event;
					CardLayout cardLayout = gameEventResourceCard.getResourceId() == 1 ? playerChoiceCard1Layout : playerChoiceCard2Layout;
					animateCardEvent(gameEventResourceCard.getResourceDeckCard(), cardLayout, playerFieldAttackLayout);
					handleUpdateDisplay = true;

				}

				else if (event instanceof GameEventDeckCard) {

					GameEventDeckCard gameEventDeckCard = (GameEventDeckCard) event;
					CardLayout cardLayout = gameEventDeckCard.getResourceId() == 1 ? playerChoiceCard1Layout : playerChoiceCard2Layout;
					animateCardEvent(gameEventDeckCard.getPlayerDeckCard(), cardLayout, playerFieldAttackLayout);
					handleUpdateDisplay = true;

				}

				else {

					onError(String.format("Unhandled event class %s", event.getClass()));

				}

			}

			Toast.makeText(this, str, Toast.LENGTH_SHORT).show();

		}

		Log.i(TAG, "Leaving displayEvents()");

		return handleUpdateDisplay;

	}

	private void initCardEventAnimation() {

		cardEventAnimationSet = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.card_animation);
		cardEventAnimationSet.setDuration(1000);
		cardEventAnimationSet.setFillAfter(false);
		cardEventAnimationSet.setFillEnabled(true);

		cardEventTranslationAnimation = new TranslateAnimation(0, 0, 0, 0);
		cardEventAnimationSet.setAnimationListener(gameAnimationListener);
		cardEventAnimationSet.addAnimation(cardEventTranslationAnimation);

	}

	private void animateCardEvent(ICard card, View sourceLayout, View destinationLayout) {

		int[] sourceCoordinates = new int[2];
		sourceLayout.getLocationInWindow(sourceCoordinates);
		int[] destinationCoordinates = new int[2];
		destinationLayout.getLocationInWindow(destinationCoordinates);
		int[] gameEventLayoutCoordinates = new int[2];
		gameEventLayout.getLocationInWindow(gameEventLayoutCoordinates);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		cardEventAnimationSet.getAnimations().set(
				0,
				new TranslateAnimation(Animation.ABSOLUTE, metrics.widthPixels / 2 - gameEventLayoutCoordinates[0], Animation.ABSOLUTE, destinationCoordinates[0]
						- gameEventLayoutCoordinates[0], Animation.ABSOLUTE, sourceCoordinates[1] - gameEventLayoutCoordinates[1], Animation.ABSOLUTE, destinationCoordinates[1]
						- gameEventLayoutCoordinates[1]));

		gameEventLayout.setup(this, card, 0, CardLocation.ANIMATION);
		gameEventLayout.startAnimation(cardEventAnimationSet);

	}

	public CardLayout getGameEventLayout() {
		return gameEventLayout;
	}

	public void setGameEventLayout(CardLayout gameEventLayout) {
		this.gameEventLayout = gameEventLayout;
	}

	@Override
	public void onGetGame(GameView gameView) {

		if (gameView == null) {
			onError("Unable to get game");
			return;
		}

		Log.i(TAG, String.format("Entering onGetGame: state=%s, phase=%s", gameView.getGameState(), gameView.getPhase()));

		gameInfos = (TextView) layout.findViewById(R.id.gameInfos);
		this.gameView = gameView;

		player = gameView.getPlayer();
		opponent = gameView.getOpponents().get(0);
		opponentPlayerLayout.setup(opponent);
		playerPlayerLayout.setup(player);
		opponentDefenseDown = opponent.getCurrentDefense() > 0 ? false : true;

		switch (gameView.getGameState()) {

			case STARTED:

				if (gameView.getPhase() == Phase.DURING_PLAY) {
					boolean handleDisplay = displayEvents();

					if (!handleDisplay) {
						updateCardsDisplay();
					}
				}
				else {
					updateCardsDisplay();
					displayEvents();
				}
				
				break;

			default:
				Log.e("onGetGame", String.format("Invalid game state: %s", gameView.getGameState()));
				break;

		}

		gameInfos.setText(String.format("%s [%s / %s]", gameView.toString(), ++httpCallsDone, httpCallsAborted));
		gameTrade.setText(String.format("%s", gameView.getTrade()));
		gameDefense.setText(String.format("%s", gameView.getDefense()));
		gameFaith.setText(String.format("%s", gameView.getFaith()));

		// Activate Next Phase only if this is the active player
		if (gameView.isActivePlayer()) {
			nextPhaseButton.setEnabled(true);
		} else {
			nextPhaseButton.setEnabled(false);
		}

		Log.i(TAG, String.format("Leaving onGetGame()"));

	}

	@Override
	public void onError(String err) {
		Toast.makeText(this, String.format("Error occured: %s", err), Toast.LENGTH_LONG).show();
	}

	public void onDeleteGame() {
		returnHome(ClientConstants.GAME_STOPPED);
		finish();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
