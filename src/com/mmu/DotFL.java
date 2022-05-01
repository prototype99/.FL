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
    boolean circleTest = true;
    float circleSize, circleX, circleY;
    PVector[] p = new PVector[0];
    int numSticksNew;
    int numSticksOld = 1;
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
        if (circleTest) {
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
            strokeWeight(10);
            for (int i = 0; i < p.length; i++) {
                PVector prev = p[i];
                if (i - 1 >= 0) {
                    prev = p[i - 1];
                }
                line(p[i].x, p[i].y, prev.x, prev.y);
            }
            stroke(128);
            circle(circleX, circleY, circleSize * 2);
            stroke(255);
        }
        //update stored value
        numSticksOld = numSticksNew;
    }

    @Override
    public void mouseReleased() {
        if (p.length > 10 && circleTest) {
            circleX = 0;
            circleY = 0;
            for (PVector v : p) {
                circleX += v.x;
                circleY += v.y;
            }
            circleX /= p.length;
            circleY /= p.length;
            circleSize = 0;
            for (PVector v : p) {
                circleSize += dist(v.x, v.y, circleX, circleY);
            }
            circleSize /= p.length;
            float errorFactor = 0;
            for (PVector v : p) {
                errorFactor += abs(dist(v.x, v.y, circleX, circleY) - circleSize);
            }
            //work out mean absolute difference and normalise to radius so it scales to size
            errorFactor = (errorFactor / p.length) / circleSize;
            if (circleSize > 24) {
                System.out.println(errorFactor);
                circleTest = false;
                background(0);
            }
        }
    }
}