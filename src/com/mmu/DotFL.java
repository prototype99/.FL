package com.mmu;

import org.libsdl.api.event.events.SDL_Event;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Objects;

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
    boolean inputMouse = true;
    //gyroV in the format: {y, x}
    float[] choir, gyroV, ps;
    GhostLog logNew, logOld = new GhostLog();
    int drawMode = 1, hitTargets, targetLoops;
    //strings are predeclared to allow some cool math later. ye, i could probably use an enum but i've never liked them. also, less rewriting memory
    String[] msgsChange = new String[3];

    public static void main(String[] args) {
        PApplet.main("com.mmu.DotFL");
    }

    @Override
    public void setup() {
        //processing setup
        surface.setSize(displayWidth, displayHeight);
        windowResizable(true);
        //higher refresh rate for smooth input
        frameRate(120);
        //we draw our own cursor
        noCursor();
    }

    @Override
    public void draw() {
        surface.setSize(width, height);
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
                                        System.out.println("down");
                                    }
                                }
                                case SDL_CONTROLLERBUTTONUP -> {
                                    if (e.cbutton.button == 0) {
                                        System.out.println("up");
                                    }
                                }
                                case SDL_CONTROLLERSENSORUPDATE -> {
                                    logNew.gyroEvent = e.csensor;
                                    if (logNew.gyroEvent.sensor == SDL_SENSOR_GYRO) {
                                        int gtime;
                                        try{
                                            gtime = logNew.gyroEvent.timestamp - logOld.gyroEvent.timestamp;
                                        } catch (NullPointerException ignored) {
                                            gtime = logNew.gyroEvent.timestamp;
                                        }
                                        if(gtime > 0) {
                                            for (int i = 0; i < 2; i++) {
                                                //invert the x axis
                                                if (i == 1) {
                                                    gtime *= -1;
                                                }
                                                //move the cursor position data
                                                gyroV[i] += logNew.gyroEvent.data[i * 2] * gtime;
                                            }
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
        //handle processing code
        background(190);
        if (inputMouse && mousePressed) {
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
                        p.add(new float[]{mouseX, mouseY});
                    }
                }
                case 3 -> {
                    targets.removeIf(t -> (isHit(t, mouseX, mouseY, 0)));
                    for (int i = 0; i < 3 - targets.size(); i++) {
                        hitTargets++;
                        addTarget();
                    }
                }
            }
        }
        switch (drawMode) {
            case 1 -> {
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
                if (targetLoops >= 7200) {
                    drawMode = 4;
                    System.out.println(hitTargets);
                } else {
                    //draw targets
                    fill(255,0,0);
                    noStroke();
                    for (float[] t : targets) {
                        circle(t[0],t[1],t[2]);
                    }
                }
                targetLoops++;
            }
        }
        //draw the cursor
        noFill();
        strokeWeight(2);
        stroke(0,255,0);
        if (inputMouse) {
            circle(mouseX, mouseY, 10);
        } else {
            float[] screenDim = new float[]{height, width};
            try {
                for (int i = 0; i < 2; i++) {
                    if (gyroV[i] < 0) {
                        gyroV[i] = 0;
                    } else if (gyroV[i] > screenDim[i]) {
                        gyroV[i] = screenDim[i];
                    }
                }
                circle(gyroV[1], gyroV[0], 10);
            } catch(NullPointerException n) {
                gyroV = screenDim;
                for (int i = 0; i < 2; i++) {
                    gyroV[i] /= 2.0F;
                }
            }
            //present becomes past
            logOld.gamepad = logNew.gamepad;
            logOld.gamepadError = logNew.gamepadError;
            logOld.gyroEvent = logNew.gyroEvent;
            logOld.gyroStatus = logNew.gyroStatus;
            logOld.numSticks = logNew.numSticks;
        }
    }

    //creates a new Target free from the past
    public void addTarget() {
        float[] t = newTarget();
        while(isIntersect(t[0], t[1], t[2]) || t[2] < 18.9) {
            t = newTarget();
        }
        targets.add(t);
    }

    public float[] newTarget() {
        return new float[]{random(width), random(height), random(120.3F)};
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

    @Override
    public void mouseReleased() {
        if (p.size() > 10 && drawMode == 1 && inputMouse) {
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
            errorFactor = (errorFactor / p.size()) / circRad;
            System.out.println(errorFactor);
            //ensure circle has enough data points for accurate analysis
            if (circRad > 24) {
                if (inputMouse) {
                    //reset the test
                    p.clear();
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
                    drawMode = 3;
                    targets = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        addTarget();
                    }
                }
            }
        }
    }

    public void song() {
        choir = new float[]{atan2(ps[1] - mouseY, ps[0] - mouseX), dist(mouseX, mouseY, ps[0], ps[1])};
    }
}