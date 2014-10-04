package ru.sig.snake.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import ru.sig.snake.R;
import ru.sig.snake.exceptions.NoViewIsSetException;
import ru.sig.snake.model.Snake;
import ru.sig.snake.model.node.FieldNode;
import ru.sig.snake.model.node.FoodNode;
import ru.sig.snake.model.node.ObstacleNode;
import ru.sig.snake.model.node.SnakeNode;
import ru.sig.snake.view.GameView;


/**
 * Created by Alexander Ionov on 27.09.14.
 */
public class GameLogic
{
    private static GameLogic instance;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_PAUSED = 2;

    private int state = STATE_STOPPED;

    private static final long DEFAULT_DELAY = 100;
    private static final long DEFAULT_SPEED = 200;
    private static final long SPEED_CHANGE_STEP = 20;

    private long snakeSpeed;
    private static final int START_SNAKE_SIZE = 4; //TODO: Use this field

    private int resultOfMove;
    
    public static final int FIELD_WIDTH = 40;      //count of nodes in width of display
    public static final int FIELD_HEIGHT = 40;     //count of nodes in height of display

    private int SNAKE_FOUND_NOTHING = 0;            //this node is free
    private int SNAKE_FOUND_FOOD = 1;            //on this node food
    private int SNAKE_FOUND_OBSTACLE = 2;        //on this node obstacle or snakeNode

    private GameView snakeView;
    private Activity activity;

    private List<FieldNode> field = new ArrayList<FieldNode>();
    private Snake snake;
    private int difficulty;
    private Timer timer;

    public static GameLogic getInstance()
    {
        if (instance == null)
        {
            synchronized (GameLogic.class)
            {
                if (instance == null)
                {
                    instance = new GameLogic();
                }
            }
        }

        return instance;
    }

    private GameLogic() { }

    public void setView(GameView view)
    {
        snakeView = view;
        activity = (Activity) snakeView.getContext();

        snakeView.setField(field);

        if (snake != null)
        {
            setOnTouchListeners();
        }
    }

    public void startGame(int difficulty)
    {
        if (snakeView == null)
        {
            throw new NoViewIsSetException();
        }

        field = new ArrayList<FieldNode>();
        snakeView.setField(field);

        snake= new Snake(field, 10, 10, 14);

        snakeSpeed = DEFAULT_SPEED;
        timer = new Timer();
        timer.schedule(new SnakeTimerTask(this), DEFAULT_DELAY, snakeSpeed) ;

        generateFood();

        setOnTouchListeners();

        SnakeMusicPlayer.getInstance().playMusic(activity);

        state = STATE_STARTED;
    }

    public void pause()
    {
        state = STATE_PAUSED;
    }

    public void resume()
    {
        state = STATE_STARTED;
    }

    public void exit()
    {
        state = STATE_STOPPED;
    }

    public int getState()
    {
        return state;
    }

    public void move()
    {
        if (snakeView == null)
        {
            throw new NoViewIsSetException();
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FieldNode result = snake.move();
                snakeView.invalidate();
                checkState(result);
            }
        });
    }

    private int checkState(FieldNode moveResult)
    {
        if (moveResult != null)
        {
            if (moveResult instanceof FoodNode)
            {
                resultOfMove = SNAKE_FOUND_FOOD;
                snake.setSatiety(2);
                generateFood();
                if (snakeSpeed > SPEED_CHANGE_STEP)
                {
                    snakeSpeed -= SPEED_CHANGE_STEP;
                }
            }
            else if (moveResult instanceof SnakeNode || moveResult instanceof ObstacleNode)
            {
                resultOfMove = SNAKE_FOUND_OBSTACLE;
            }
        }
        else
        {
            resultOfMove = SNAKE_FOUND_NOTHING;
        }
        if (isSnakeReachedBorder())
        {
            resultOfMove = SNAKE_FOUND_OBSTACLE;
        }

        if (resultOfMove == SNAKE_FOUND_OBSTACLE)
        {
            gameover();
        }

        return resultOfMove;
    }

    private void generateFood()
    {
        Random random = new Random();
        if (field.size() <= FIELD_HEIGHT * FIELD_WIDTH)
        {
            FoodNode node;
            do
            {
                int foodx = random.nextInt(FIELD_WIDTH);
                int foody = random.nextInt(FIELD_HEIGHT);
                node = new FoodNode(foodx, foody);
            }
            while (field.contains(node));

            field.add(node);
        }
    }

    private boolean isSnakeReachedBorder()
    {
        FieldNode headSnake = snake.getHead();

        if (headSnake.getX() == FIELD_WIDTH | headSnake.getY() == FIELD_HEIGHT
                | headSnake.getX() == -1 | headSnake.getY() == -1)
            return true;

        return false;
    }

    private void gameover()
    {
        timer.cancel();
        SnakeMusicPlayer.getInstance().playGameOverSound(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(activity.getString(R.string.loseTitle));
        builder.setMessage(activity.getString(R.string.loseMessage));
        builder.setPositiveButton(activity.getString(R.string.yes),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startGame(0);
            }
        });
        builder.setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.finish();
                dialogInterface.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    private void setOnTouchListeners()
    {
        snakeView.setOnTouchListener(new OnSwipeTouchListener(activity.getApplicationContext()) {
            public void onSwipeTop() {
                if (Snake.DIRECTION_SOUTH != snake.getDirection())
                    snake.setDirection(Snake.DIRECTION_NORTH);
            }

            public void onSwipeRight() {
                if (Snake.DIRECTION_WEST != snake.getDirection())
                    snake.setDirection(Snake.DIRECTION_EAST);
            }

            public void onSwipeLeft() {
                if (Snake.DIRECTION_EAST != snake.getDirection())
                    snake.setDirection(Snake.DIRECTION_WEST);
            }

            public void onSwipeBottom() {
                if (Snake.DIRECTION_NORTH != snake.getDirection())
                    snake.setDirection(Snake.DIRECTION_SOUTH);
            }
        });
    }

}
