package com.mmu;

import org.libsdl.api.event.events.SDL_ControllerSensorEvent;
import org.libsdl.api.event.events.SDL_Event;
import processing.core.PApplet;
import processing.core.PVector;

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
    ArrayList<float[]> targets;
    GhostLog logNew, logOld = new GhostLog();
    int drawMode = 1, hitTargets, targetLoops;
    PVector[] p;
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
            targets.removeIf(t -> (isHit(t, mouseX, mouseY, 0)));
            for (int i = 0; i < 3 - targets.size(); i++) {
                hitTargets++;
                addTarget();
            }
            redraw();
        }
    }

    @Override
    public void mouseReleased() {
        if (p.length > 10 && drawMode == 1) {
            float circX = 0, circY = 0;
            for (PVector v : p) {
                circX += v.x;
                circY += v.y;
            }
            circX /= p.length;
            circY /= p.length;
            float circRad = 0;
            for (PVector v : p) {
                circRad += dist(v.x, v.y, circX, circY);
            }
            circRad /= p.length;
            float errorFactor = 0;
            for (PVector v : p) {
                errorFactor += abs(dist(v.x, v.y, circX, circY) - circRad);
            }
            //work out mean absolute difference and normalise to radius so it scales to size
            //a higher error factor equates to more errors
            errorFactor = (errorFactor / p.length) / circRad;
            //ensure circle has enough data points for accurate analysis
            if (circRad > 24) {
                System.out.println(errorFactor);
                drawMode = 3;
                targets = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    addTarget();
                }
                redraw();
            }
        }
    }

    //this is needed to ensure layering is done properly
    public void redraw() {
        surface.setSize(width, height);
        logNew = new GhostLog();
        logNew.numSticks = SDL_NumJoysticks();
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
                        SDL_ControllerSensorEvent es;
                        SDL_PollEvent(e);
                        if (e.type == SDL_CONTROLLERSENSORUPDATE) {
                            es = e.csensor;
                            if (es.sensor == SDL_SENSOR_GYRO) {
                                System.out.println(Arrays.toString(es.data));
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
        //handle processing code
        background(190);
        switch (drawMode) {
            case 1 -> {
                if (mousePressed) {
                    if (p.length > 0) {
                        PVector ps = p[p.length - 1];
                        float angle = atan2(ps.y - mouseY, ps.x - mouseX);
                        float dis = dist(mouseX, mouseY, ps.x, ps.y);
                        while (dis > 5) {
                            p = (PVector[]) append(p, new PVector(ps.x - 5 * cos(angle), ps.y - 5 * sin(angle)));
                            ps = p[p.length - 1];
                            angle = atan2(ps.y - mouseY, ps.x - mouseX);
                            dis = dist(mouseX, mouseY, ps.x, ps.y);
                        }
                    } else {
                        p = (PVector[]) append(p, new PVector(mouseX, mouseY));
                    }
                }
                //do all the actual drawing
                stroke(255);
                strokeWeight(10);
                try {
                    for (int i = 0; i < p.length; i++) {
                        PVector prev = p[i];
                        if (i - 1 >= 0) {
                            prev = p[i - 1];
                        }
                        line(p[i].x, p[i].y, prev.x, prev.y);
                    }
                } catch(NullPointerException n) {
                    p = new PVector[0];
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
        circle(mouseX, mouseY, 10);
        //present becomes past
        logOld.gamepad = logNew.gamepad;
        logOld.gamepadError = logNew.gamepadError;
        logOld.gyroStatus = logNew.gyroStatus;
        logOld.numSticks = logNew.numSticks;
    }
}