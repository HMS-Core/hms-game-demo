/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    
 *  http://www.apache.org/licenses/LICENSE-2.0
 *    
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.huawei.gamecenter.minigame.huawei.UI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.huawei.gamecenter.minigame.huawei.GameControler;
import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.model.Artillery;
import com.huawei.gamecenter.minigame.huawei.model.Bullet;
import com.huawei.gamecenter.minigame.huawei.model.Enemy;
import com.huawei.gamecenter.minigame.huawei.model.MyPoint;
import com.huawei.gamecenter.minigame.huawei.model.MyRect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.Nullable;

public class MyCustomView extends View implements GameControler {
    private static final String TAG = "MyCustomView";
    public static Boolean isRefresh = true;
    private final int screenWidth;
    private final int cordon;
    private final int screenHeight;
    private boolean isSpawning = false;
    private float moveSpd = (float) 1.5;

    private Bitmap bmScaled;
    private Bullet bullet;
    private Artillery artilleryObj;
    private Enemy enemy;
    private Bitmap bmPaoDan;
    private Bitmap bmDaoDan;
    private Bitmap bmBoom;

    private final List<MyPoint> bulletPoints = new ArrayList<>();
    private final List<MyPoint> enemyPoints = new ArrayList<>();
    private final Random positionRand = new Random();
    private final Runnable spawnRunnable = this::instantiateEnemy;
    private MyCustomView.BeatEnemyListener beatEnemyListener;

    public MyCustomView(@Nullable Context context) {
        this(context, null);
    }

    public MyCustomView(@Nullable Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        DisplayMetrics dm;
        dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        cordon = (int) (screenHeight * 0.75);
        init();
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void init() {
        Bitmap bmSrc = BitmapFactory.decodeResource(getResources(), R.mipmap.paotai);
        Matrix matrix = new Matrix();
        matrix.setScale(0.6f, 0.6f);
        bmScaled = Bitmap.createBitmap(bmSrc, 0, 0, bmSrc.getWidth(), bmSrc.getHeight(), matrix, true);
        artilleryObj = new Artillery(new Matrix(), new Paint(), bmScaled);
        artilleryObj.setCenter(screenWidth / 2, screenHeight - artilleryObj.getBitmap().getHeight() / 5);
        artilleryObj.getMatrix().postTranslate(artilleryObj.getCenterX() - artilleryObj.getBitmap().getWidth() / 2, artilleryObj.getCenterY() - (int) (artilleryObj.getBitmap().getHeight() * 0.7));
        bmDaoDan = BitmapFactory.decodeResource(getResources(), R.mipmap.daodang);
        bmDaoDan = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.daodang),
                0, 0, bmDaoDan.getWidth(), bmDaoDan.getHeight(), matrix, true);
        Matrix matrixBoom = new Matrix();
        matrix.setScale(0.9f, 0.9f);
        bmBoom = BitmapFactory.decodeResource(getResources(), R.mipmap.boom);
        bmBoom = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.boom),
                0, 0, bmBoom.getWidth(), bmBoom.getHeight(), matrixBoom, true);

        Paint enemyPaint = new Paint();
        enemy = new Enemy(enemyPaint, moveSpd, bmDaoDan.getWidth() / 2, bmDaoDan);

        Paint bulletPaint = new Paint();
        Matrix matrixPd = new Matrix();
        matrixPd.setScale(0.65f, 0.65f);
        bmPaoDan = BitmapFactory.decodeResource(getResources(), R.mipmap.paodan);
        bmPaoDan = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.paodan),
                0, 0, bmPaoDan.getWidth(), bmPaoDan.getHeight(), matrixPd, true);
        bullet = new Bullet(bulletPaint, bmPaoDan.getWidth() / 2, 6, bmPaoDan);
    }

    /**
     * @param canvas UI界面绘制逻辑，各个模型初始化绘制。
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawShot(canvas);
    }

    @SuppressWarnings({"IntegerDivisionInFloatingPointContext", "UnusedAssignment"})
    private void drawShot(Canvas canvas) {
        Paint pLine = new Paint();
        pLine.setColor(Color.RED);
        canvas.drawLine(0, cordon, screenWidth, cordon, pLine);
        artilleryObj.getMatrix().reset();
        artilleryObj.getMatrix().postTranslate(artilleryObj.getCenterX() - artilleryObj.getBitmap().getWidth() / 2, artilleryObj.getCenterY() - (int) (artilleryObj.getBitmap().getHeight() * 0.7));
        canvas.drawBitmap(artilleryObj.getBitmap(), artilleryObj.getMatrix(), artilleryObj.getPaint());
        for (int i = 0; i < enemyPoints.size(); i++) {
            if (enemyPoints.get(i).getY() + bmBoom.getHeight() / 2 >= cordon) {
                // 炮弹越过警戒线游戏终止
                canvas.drawBitmap(bmBoom, enemyPoints.get(i).getX(), enemyPoints.get(i).getY(), enemy.getPaint());
                gmEnd();
                enemyPoints.remove(i--);
                break;
            }
            if (enemyPoints.get(i).isOutOfBoundsWithOutTop(screenWidth, screenHeight - bmScaled.getHeight())) {
                enemyPoints.remove(i--);
            } else {
                canvas.drawBitmap(enemy.getBitmap(), enemyPoints.get(i).getX(), enemyPoints.get(i).getY(), enemy.getPaint());
                if (!isRefresh) {
                    enemyPoints.get(i).move(enemy.getMoveStep(), true);
                }
            }
        }

        for (int i = 0; i < bulletPoints.size(); i++) {
            if (bulletPoints.get(i).isOutOfBounds(screenWidth, screenHeight)) {
                bulletPoints.remove(i--);
            } else {
                // 移动所有的点
                canvas.drawBitmap(bullet.getBitmap(), bulletPoints.get(i).getX() - bullet.getBitmap().getWidth() / 2, bulletPoints.get(i).getY() - (int) (artilleryObj.getBitmap().getHeight() * 0.9), bullet.getPaint());
                if (!isRefresh) {
                    bulletPoints.get(i).move(bullet.getMoveStep() + 1, false);
                }
                // 是否发生碰撞
                for (int j = 0; j < enemyPoints.size(); j++) {
                    MyRect r = new MyRect();
                    r.setBounds(bulletPoints.get(i).getX() - bullet.getBitmap().getWidth() / 2, bulletPoints.get(i).getY() - (int) (artilleryObj.getBitmap().getHeight() * 0.9), (int) (bmPaoDan.getWidth() * 0.4), (int) (bmPaoDan.getHeight() * 0.7));
                    MyRect p = new MyRect();
                    p.setBounds(enemyPoints.get(j).getX(), enemyPoints.get(j).getY(), (int) (bmDaoDan.getWidth() * 0.5), (int) (bmDaoDan.getHeight() * 0.6));
                    if (r.allIntersects(p)) {
                        canvas.drawBitmap(bmBoom, enemyPoints.get(j).getX(), enemyPoints.get(j).getY(), enemy.getPaint());
                        // 移除子弹
                        bulletPoints.remove(i--);
                        // 移除敌人
                        enemyPoints.remove(j);
                        // 发生监听事件
                        if (beatEnemyListener != null)
                            beatEnemyListener.onBeatEnemy(0);
                        break;
                    }

                }
            }

        }

        // 如果敌人未到达最大数量，并且不在生成敌人，继续生成敌人,并且游戏未开始
        // 敌人的最大数量
        int maxEnemyNum = 6;
        if (enemyPoints.size() < maxEnemyNum && !isSpawning && !isRefresh) {
            isSpawning = true;
            postDelayed(spawnRunnable, 1000);
        }

        postInvalidateDelayed(100);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 计算水平轴与 大炮中心和点击的位置连成的直线 之间的夹角,暂时不考虑此变化。
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 炮台旋转方向
            float currentRotate;
            if ((event.getY() > artilleryObj.getCenterY()) & (event.getX() > artilleryObj.getCenterX()) || (event.getY() > artilleryObj.getCenterY()) & (artilleryObj.getCenterX() > event.getX())) {
                currentRotate = (float) Math.toDegrees(Math.atan(-(artilleryObj.getCenterY() - event.getY()) / (event.getX() - artilleryObj.getCenterX())));
            } else if (event.getY() == artilleryObj.getCenterY() || event.getX() == artilleryObj.getCenterX()) {
                currentRotate = (float) Math.toDegrees(Math.atan(0));
            } else {
                currentRotate = (float) Math.toDegrees(Math.atan((artilleryObj.getCenterY() - event.getY()) / (event.getX() - artilleryObj.getCenterX())));
            }

            // 将点击的位置放入点的集合中,暂时不控制，作为bug功能
            if (!isRefresh) {
                bulletPoints.add(new MyPoint(artilleryObj.getCenterX(), artilleryObj.getCenterY(), Math.toRadians(currentRotate)));
                beatEnemyListener.onFire();
            }
        }
        return true;
    }

    // 生成导弹 instantiateEnemy
    private void instantiateEnemy() {
        if (!isRefresh) {
            enemyPoints.add(new MyPoint(positionRand.nextInt(screenWidth - enemy.getBitmap().getWidth()) + enemy.getBitmap().getWidth() / 2, -50, Math.toRadians(-90)));
        }
        isSpawning = false;
    }

    public void setBeatEnemyListener(BeatEnemyListener listener) {
        this.beatEnemyListener = listener;
    }

    /**
     * 此处为控制游戏界面是否继续绘制的控制标识开关。
     */
    @Override
    public void gameSwitch(Boolean refresh) {
        isRefresh = refresh;
    }

    @Override
    public void setGameDifficulty(int gameLevel) {
        HMSLogHelper.getSingletonInstance().debug(TAG, "Game difficult level is:" + gameLevel);
    }

    @Override
    public void setAbscissa(int dpX) {
        // 设置炮台左右的位置坐标
        artilleryObj.setCenterX(artilleryObj.getCenterX() + dpX);
    }

    @Override
    public void setEmSpd(int spd, int level) {
        bulletPoints.clear();
        enemyPoints.clear();
        if (spd == Constant.MODE_THREE) {
            moveSpd = (float) (moveSpd + 0.2 * level);
            enemy.setMoveStep(moveSpd);
        }
        if (spd == Constant.M0DE_TWO) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "moveSpd : " + moveSpd);
        }
    }

    private void gmEnd() {
        isRefresh = true;
        if (beatEnemyListener != null)
            beatEnemyListener.gameEnd(Constant.MODE_ONE);
    }

    // 设计击中敌人时的监听
    public interface BeatEnemyListener {
        /**
         * @param showMode 后续通过这个数值进行判断是否终止游戏，目前暂时定为消灭目标数量
         */
        void onBeatEnemy(int showMode);

        void onFire();

        void gameEnd(int i);
    }
}
