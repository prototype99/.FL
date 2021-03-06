package com.mmu;

import org.libsdl.api.event.events.SDL_Event;
import processing.core.PApplet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_GAMECONTROLLER;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERBUTTONDOWN;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERBUTTONUP;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERSENSORUPDATE;
import static org.libsdl.api.event.SdlEvents.SDL_PollEvent;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerHasSensor;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerOpen;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerSetSensorEnabled;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_IsGameController;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;
import static org.libsdl.api.sensor.SDL_SensorType.SDL_SENSOR_GYRO;

public class DotFL extends PApplet {
    //in the format: {x, y, size}
    ArrayList<float[]> p;
    //TODO: remove this
    ArrayList<float[]> targets;
    boolean gyroPressed = false, inputMouse = true, timeElapsed;
    //dims/gyroV in the format: {y, x}
    float[] choir, dims, gyroCur, gyroVel, ps;
    GhostLog logNew, logOld = new GhostLog();
    int drawMode = 1, hitTargets;
    //strings are predeclared to allow some cool math later. ye, i could probably use an enum but i've never liked them. also, less rewriting memory
    String[] msgsChange = new String[3];
    String userResults = "circle|mouse|";
    Timer timer;

    public static void main(String[] args) {
        PApplet.main("com.mmu.DotFL");
    }

    @Override
    public void setup() {
        //processing setup
        surface.setSize(displayWidth, displayHeight);
        windowResizable(true);
        //higher refresh rate for smooth input
        frameRate(480);
        //we draw our own cursor
        noCursor();
        textSize(50);
    }

    @Override
    public void draw() {
        surface.setSize(width, height);
        //handle processing code
        background(190);
        if (!inputMouse) {
            try {
                logNew.numSticks = SDL_NumJoysticks();
            } catch(NullPointerException n) {
                logNew = new GhostLog();
            }
            //output a message if required
            if (logNew.numSticks != logOld.numSticks) {
                int joyStatus = logNew.numSticks;
                if (joyStatus > 2) {
                    joyStatus = 2;
                }
                System.out.println(msgsChange[joyStatus]);
            }
            //Check for joysticks
            if (logNew.numSticks > 0) {
                for (int i = 0; i < logNew.numSticks; i++) {
                    if (SDL_IsGameController(i)) {
                        //Load DS5
                        logNew.gamepad = SDL_GameControllerOpen(i);
                    }
                }
                if (logNew.gamepad == null) {
                    logNew.gamepadError = SDL_GetError();
                    if (logNew.gamepad != logOld.gamepad || !Objects.equals(logNew.gamepadError, logOld.gamepadError)) {
                        System.out.println("Warning: Unable to open gamepad! SDL Error:" + logNew.gamepadError);
                    }
                } else {
                    logNew.gyroStatus = SDL_GameControllerHasSensor(logNew.gamepad, SDL_SENSOR_GYRO);
                    if (logNew.gyroStatus) {
                        if (SDL_GameControllerSetSensorEnabled(logNew.gamepad, SDL_SENSOR_GYRO, true) == -1) {
                            System.out.println("Warning: unable to enable gyroscope");
                        } else {
                            SDL_Event e = new SDL_Event();
                            SDL_PollEvent(e);
                            switch(e.type) {
                                case SDL_CONTROLLERBUTTONDOWN -> {
                                    if (e.cbutton.button == 0) {
                                        if (!gyroPressed) {
                                            gyroPressed = true;
                                        }
                                    }
                                }
                                case SDL_CONTROLLERBUTTONUP -> {
                                    if (e.cbutton.button == 0) {
                                        if (gyroPressed) {
                                            gyroPressed = false;
                                            releaseEvent();
                                        }
                                    }
                                }
                                case SDL_CONTROLLERSENSORUPDATE -> {
                                    logNew.gyroEvent = e.csensor;
                                    if (logNew.gyroEvent.sensor == SDL_SENSOR_GYRO) {
                                        int pol = 1;
                                        for (int i = 0; i < 2; i++) {
                                            //invert the x axis
                                            if (i == 1) {
                                                pol *= -1;
                                            }
                                            //move the cursor position data
                                            try{
                                                gyroVel[i] += logNew.gyroEvent.data[i * 2] * pol;
                                            } catch(NullPointerException n) {
                                                gyroVel = new float[2];
                                            }
                                            gyroCur[i] += gyroVel[i] / 3;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (logNew.gyroStatus != logOld.gyroStatus) {
                            System.out.println("Warning: no gyroscope detected, did you connect the right controller?");
                        }
                    }
                }
            }
        }
        if (isPressed()) {
            switch (drawMode) {
                case 1 -> {
                    if (p.size() > 0) {
                        ps = p.get(p.size() - 1);
                        song();
                        while (choir[1] > 5) {
                            float[] angles = new float[]{cos(choir[0]), sin(choir[0])};
                            for (int i = 0; i < 2; i++) {
                                angles[i] = ps[i] - 5 * angles[i];
                            }
                            p.add(angles);
                            ps = angles;
                            song();
                        }
                    } else {
                        getDims();
                        p.add(new float[]{dims[1], dims[0]});
                    }
                }
                case 3 -> {
                    getDims();
                    targets.removeIf(t -> (isHit(t, dims[1], dims[0], 0)));
                    for (int i = 0; i < 3 - targets.size(); i++) {
                        hitTargets++;
                        addTarget();
                    }
                }
            }
        }
        switch (drawMode) {
            case 1 -> {
                if (!isPressed()) {
                    fill(0,0,255);
                    String circleString = "please draw a circle with your ";
                    if (inputMouse) {
                        circleString += "mouse";
                    } else {
                        circleString += "gyro (X/A to draw)";
                    }
                    text(circleString, width/8.0F, height/2.0F);
                }
                //do all the actual drawing
                stroke(255);
                strokeWeight(10);
                try {
                    for (int i = 0; i < p.size(); i++) {
                        float[] prev = p.get(i);
                        if (i - 1 >= 0) {
                            prev = p.get(i - 1);
                        }
                        float[] pl = p.get(i);
                        line(pl[0], pl[1], prev[0], prev[1]);
                    }
                } catch(NullPointerException n) {
                    p = new ArrayList<>();
                }
            }
            case 3 -> {
                if (timeElapsed) {
                    userResults += hitTargets;
                    if (inputMouse) {
                        userResults += "|gyro|";
                        inputMouse = false;
                        centreGyro();
                        hitTargets = 0;
                        startTargets();
                    } else {
                        drawMode = 4;
                        //obfuscateeee
                        userResults = Base64.getEncoder().encodeToString(userResults.getBytes(StandardCharsets.UTF_8));
                        System.out.println(userResults);
                    }
                } else {
                    if (hitTargets == 0) {
                        fill(0,0,255);
                        String targetString = "please hit targets with your ";
                        if (inputMouse) {
                            targetString += "mouse";
                        } else {
                            targetString += "gyro (X/A to shoot)";
                        }
                        text(targetString, width/8.0F, height/2.0F);
                    }
                    //draw targets
                    fill(255,0,0);
                    noStroke();
                    for (float[] t : targets) {
                        circle(t[0],t[1],t[2]);
                    }
                }
            }
            case 4 -> {
                fill(0,0,255);
                //get the middle of the String
                final int mid = userResults.length() / 2;
                String[] parts = {userResults.substring(0, mid),userResults.substring(mid)};
                text(parts[0], width/8.0F, height/2.0F);
                text(parts[1], width/8.0F, height/2.0F+50);
            }
        }
        //draw the cursor
        noFill();
        strokeWeight(2);
        stroke(0);
        if (inputMouse) {
            circle(mouseX, mouseY, 10);
        } else {
            float[] screenDim = new float[]{height, width};
            try {
                for (int i = 0; i < 2; i++) {
                    if (gyroCur[i] < 0) {
                        gyroCur[i] = 0;
                    } else if (gyroCur[i] > screenDim[i]) {
                        gyroCur[i] = screenDim[i];
                    }
                }
                circle(gyroCur[1], gyroCur[0], 10);
            } catch(NullPointerException n) {
                centreGyro();
            }
            //present becomes past
            logOld.gamepad = logNew.gamepad;
            logOld.gamepadError = logNew.gamepadError;
            logOld.gyroEvent = logNew.gyroEvent;
            logOld.gyroStatus = logNew.gyroStatus;
            logOld.numSticks = logNew.numSticks;
        }
    }

    public void centreGyro () {
        //grab screen size
        gyroCur = new float[]{height, width};
        for (int i = 0; i < 2; i++) {
            gyroCur[i] /= 2.0F;
        }
    }

    public boolean isHit(float[] t, float x, float y, float c) {
        return dist(x, y, t[0], t[1]) < (c + t[2]) / 2;
    }

    public boolean isIntersect(float x, float y, float c) {
        for (float[] t : targets) {
            if (isHit(t, x, y, c)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPressed() {
        if (inputMouse) {
            return mousePressed;
        } else {
            return gyroPressed;
        }
    }

    public float[] newTarget() {
        return new float[]{random(width), random(height), random(120.3F)};
    }

    //creates a new Target free from the past
    public void addTarget() {
        float[] t = newTarget();
        while(isIntersect(t[0], t[1], t[2]) || t[2] < 18.9) {
            t = newTarget();
        }
        targets.add(t);
    }

    public void getDims() {
        if (inputMouse) {
            dims = new float[]{mouseY, mouseX};
        } else {
            dims = gyroCur;
        }
    }

    @Override
    public void mouseReleased() {
        releaseEvent();
    }

    public void releaseEvent () {
        if (p.size() > 10 && drawMode == 1) {
            float[] circV = new float[2];
            for (int i = 0; i < 2; i++) {
                for (float[] v : p) {
                    circV[i] += v[i];
                }
                circV[i] /= p.size();
            }
            float[] dists = new float[p.size()];
            float[] pd;
            for (int i = 0; i < p.size(); i++) {
                pd = p.get(i);
                dists[i] = dist(pd[0], pd[1], circV[0], circV[1]);
            }
            float circRad = 0;
            for (float d : dists) {
                circRad += d;
            }
            circRad /= p.size();
            float errorFactor = 0;
            for (float d : dists) {
                errorFactor += abs(d - circRad);
            }
            //work out mean absolute difference and normalise to radius so it scales to size
            //a higher error factor equates to more errors
            userResults += (errorFactor / p.size()) / circRad;
            //ensure circle has enough data points for accurate analysis
            if (circRad > 24) {
                if (inputMouse) {
                    //reset the test
                    p.clear();
                    userResults += "|gyro|";
                    inputMouse = false;
                    //repetition free string construction~
                    msgsChange[0] = "no ";
                    for (int i = 0; i < 3; i++) {
                        msgsChange[i] += "gamepad";
                    }
                    msgsChange[2] += "s";
                    for (int i = 0; i < 3; i++) {
                        msgsChange[i] += " connected";
                    }
                    //initialise sdl subsystems
                    if ((SDL_Init(SDL_INIT_GAMECONTROLLER) == -1)) {
                        System.out.println(SDL_GetError());
                    }
                } else {
                    userResults += "|target|mouse|";
                    drawMode = 3;
                    inputMouse = true;
                    startTargets();
                }
            }
        }
    }

    public void song() {
        getDims();
        choir = new float[]{atan2(ps[1] - dims[0], ps[0] - dims[1]), dist(dims[1], dims[0], ps[0], ps[1])};
    }

    public void startTargets() {
        timeElapsed = false;
        targets = new ArrayList<>();
        targetTimer(60);
        for (int i = 0; i < 3; i++) {
            addTarget();
        }
    }

    public void targetTimer(int seconds) {
        timer = new Timer();
        timer.schedule(new TargetTask(), seconds* 1000L);
    }

    class TargetTask extends TimerTask {
        @Override
        public void run() {
            timeElapsed = true;
            timer.cancel();
        }
    }
}