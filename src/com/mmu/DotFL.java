package com.mmu;

import org.libsdl.api.event.events.SDL_Event;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_GAMECONTROLLER;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERSENSORUPDATE;
import static org.libsdl.api.event.SdlEvents.SDL_PollEvent;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerHasSensor;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerOpen;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerSetSensorEnabled;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_IsGameController;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;
import static org.libsdl.api.sensor.SDL_SensorType.SDL_SENSOR_GYRO;

public class DotFL extends PApplet {
    //stored in the format: {x, y, size}
    ArrayList<float[]> p;
    ArrayList<float[]> targets;
    boolean inputMouse = true;
    float gyroX, gyroY;
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
        noCursor();
        redraw();
    }

    @Override
    public void draw() {
        redraw();
        if (drawMode == 3) {
            targetLoops++;
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
    public void mouseDragged() {
        redraw();
    }

    @Override
    public void mouseMoved() {
        redraw();
    }

    @Override
    public void mousePressed() {
        if (drawMode == 3) {
            if (inputMouse) {
                targets.removeIf(t -> (isHit(t, mouseX, mouseY, 0)));
                for (int i = 0; i < 3 - targets.size(); i++) {
                    hitTargets++;
                    addTarget();
                }
            }
        }
        redraw();
    }

    @Override
    public void mouseReleased() {
        if (inputMouse) {
            if (p.size() > 10 && drawMode == 1) {
                float[] circV = new float[2];
                for (int i = 0; i < 2; i++) {
                    for (float[] v : p) {
                        circV[i] += v[i];
                    }
                    circV[i] /= p.size();
                }
                float circRad = 0;
                //you might be able to reuse the dist!!!
                for (float[] v : p) {
                    circRad += dist(v[0], v[1], circV[0], circV[1]);
                }
                circRad /= p.size();
                float errorFactor = 0;
                for (float[] v : p) {
                    errorFactor += abs(dist(v[0], v[1], circV[0], circV[1]) - circRad);
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
                        //switch to gyro input
                        gyroX = width / 2.0F;
                        gyroY = height / 2.0F;
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
        redraw();
    }

    //this is needed to ensure layering is done properly
    public void redraw() {
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
                            if (e.type == SDL_CONTROLLERSENSORUPDATE) {
                                logNew.gyroEvent = e.csensor;
                                if (logNew.gyroEvent.sensor == SDL_SENSOR_GYRO) {
                                    int gtime;
                                    try{
                                        gtime = logNew.gyroEvent.timestamp - logOld.gyroEvent.timestamp;
                                    } catch (NullPointerException ignored) {
                                        gtime = logNew.gyroEvent.timestamp;
                                    }
                                    if(gtime > 0) {
                                        System.out.println(Arrays.toString(logNew.gyroEvent.data));
                                        gyroX += logNew.gyroEvent.data[2] * gtime;
                                        gyroY += logNew.gyroEvent.data[0] * gtime;
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
        switch (drawMode) {
            case 1 -> {
                if (inputMouse && mousePressed) {
                    if (p.size() > 0) {
                        float[] ps = p.get(p.size() - 1);
                        float angle = atan2(ps[1] - mouseY, ps[0] - mouseX);
                        float dis = dist(mouseX, mouseY, ps[0], ps[1]);
                        while (dis > 5) {
                            p.add(new float[]{ps[0] - 5 * cos(angle), ps[1] - 5 * sin(angle)});
                            ps = p.get(p.size() - 1);
                            angle = atan2(ps[1] - mouseY, ps[0] - mouseX);
                            dis = dist(mouseX, mouseY, ps[0], ps[1]);
                        }
                    } else {
                        p.add(new float[]{mouseX, mouseY});
                    }
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
                if (targetLoops >= 3600) {
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
            }
        }
        //draw the cursor
        noFill();
        strokeWeight(2);
        stroke(0,255,0);
        if (inputMouse) {
            circle(mouseX, mouseY, 10);
        } else {
            circle(gyroX, gyroY, 10);
            //present becomes past
            logOld.gamepad = logNew.gamepad;
            logOld.gamepadError = logNew.gamepadError;
            logOld.gyroEvent = logNew.gyroEvent;
            logOld.gyroStatus = logNew.gyroStatus;
            logOld.numSticks = logNew.numSticks;
        }
    }
}