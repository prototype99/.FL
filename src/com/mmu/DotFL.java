package com.mmu;

import org.libsdl.api.event.events.SDL_ControllerSensorEvent;
import org.libsdl.api.event.events.SDL_Event;
import org.libsdl.api.gamecontroller.SDL_GameController;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.Arrays;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_GAMECONTROLLER;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.event.SdlEvents.SDL_CONTROLLERSENSORUPDATE;
import static org.libsdl.api.event.SdlEvents.SDL_PollEvent;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerClose;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerHasSensor;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerOpen;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerSetSensorEnabled;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_IsGameController;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;
import static org.libsdl.api.sensor.SDL_SensorType.SDL_SENSOR_GYRO;

public class DotFL extends PApplet {
    boolean circTest = true, targetTest = false;
    float[] sizes;
    PVector[] p = new PVector[0];
    int numSticksNew, numSticksOld = 1;
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
        background(0);
        cursor(CROSS);
        noFill();
        stroke(255);
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
    }

    @Override
    public void draw() {
        surface.setSize(width, height);
        numSticksNew = SDL_NumJoysticks();
        //output a message if required
        if (numSticksNew != numSticksOld) {
            int joyStatus = numSticksNew;
            if (joyStatus > 2) {
                joyStatus = 2;
            }
            System.out.println(msgsChange[joyStatus]);
        }
        //Check for joysticks
        if (numSticksNew > 0) {
            SDL_GameController DS5 = null;
            for (int i = 0; i < numSticksNew; i++) {
                if (SDL_IsGameController(i)) {
                    //Load DS5
                    DS5 = SDL_GameControllerOpen(i);
                }
            }
            if (DS5 == null) {
                System.out.println("Warning: Unable to open gamepad! SDL Error:" + SDL_GetError());
            } else {
                if (SDL_GameControllerHasSensor(DS5, SDL_SENSOR_GYRO)) {
                    if (SDL_GameControllerSetSensorEnabled(DS5, SDL_SENSOR_GYRO, true) == -1) {
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
                    System.out.println("Warning: no gyroscope detected, did you connect the right controller?");
                }
                SDL_GameControllerClose(DS5);
            }
        }
        if (mousePressed) {
            if (targetTest) {
                for (int i = 0; i < 3; i++) {
                    if(dist(mouseX, mouseY, p[i].x, p[i].y) < sizes[i]/2) {
                        fill(190);
                        circle(p[i].x,p[i].y,sizes[i]+2);
                        genesisOfTarget(i);
                    }
                }
            } else if (circTest) {
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
        }
        if (circTest) {
            //do all the actual drawing
            strokeWeight(10);
            for (int i = 0; i < p.length; i++) {
                PVector prev = p[i];
                if (i - 1 >= 0) {
                    prev = p[i - 1];
                }
                line(p[i].x, p[i].y, prev.x, prev.y);
            }
        }
        //update stored value
        numSticksOld = numSticksNew;
    }

    //creates a new Target free from the past
    public void genesisOfTarget(int i) {
        fill(255,0,0);
        p[i] = new PVector(random(width),random(height));
        sizes[i] = random(120.3F);
        circle(p[i].x,p[i].y,sizes[i]);
    }

    @Override
    public void mouseReleased() {
        if (p.length > 10 && circTest) {
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
                circTest = false;
                targetTest = true;
                p = new PVector[3];
                sizes = new float[3];
                background(190);
                noStroke();
                for (int i = 0; i < 3; i++) {
                    genesisOfTarget(i);
                }
            }
        }
    }
}