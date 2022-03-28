package com.mmu;
import org.libsdl.api.event.events.SDL_ControllerSensorEvent;
import org.libsdl.api.event.events.SDL_Event;
import org.libsdl.api.gamecontroller.SDL_GameController;
import processing.core.PApplet;
import java.util.ArrayList;
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
    int numSticksNew;
    int numSticksOld = 1;
    //strings are predeclared to allow some cool math later. ye, i could probably use an enum but i've never liked them. also, less rewriting memory
    String[] msgsChange = new String[3];
    public static void main(String[] args) {
        PApplet.main("com.mmu.DotFL");
    }
    @Override
    public void setup() {
        clear();
        windowResizable(true);
        surface.setSize(displayWidth, displayHeight);
        cursor(CROSS);
        //repetition free string construction~
        msgsChange[0] = "no ";
        for(int i = 0; i < 3; i++) {
            msgsChange[i] += "gamepad";
        }
        msgsChange[2] += "s";
        for(int i = 0; i < 3; i++) {
            msgsChange[i] += " connected";
        }
        //initialise sdl subsystems
        if((SDL_Init(SDL_INIT_GAMECONTROLLER) == -1)) {
            System.out.println(SDL_GetError());
        }
    }
    @Override
    public void draw() {
        clear();
        surface.setSize(width, height);
        numSticksNew = SDL_NumJoysticks();
        //output a message if required
        if(numSticksNew != numSticksOld) {
            int joyStatus = numSticksNew;
            if(joyStatus > 2) {
                joyStatus = 2;
            }
            System.out.println(msgsChange[joyStatus]);
        }
        //Check for joysticks
        if(numSticksNew > 0) {
            SDL_GameController DS5 = null;
            for(int i = 0; i < numSticksNew; i++) {
                if(SDL_IsGameController(i)) {
                    //Load DS5
                    DS5 = SDL_GameControllerOpen(i);
                }
            }
            if(DS5 == null) {
                System.out.println("Warning: Unable to open gamepad! SDL Error:" + SDL_GetError());
            } else {
                if(SDL_GameControllerHasSensor(DS5, SDL_SENSOR_GYRO)) {
                    if(SDL_GameControllerSetSensorEnabled(DS5, SDL_SENSOR_GYRO, true) == -1) {
                        System.out.println("Warning: unable to enable gyroscope");
                    } else {
                        SDL_Event e = new SDL_Event();
                        SDL_ControllerSensorEvent es;
                        SDL_PollEvent(e);
                        if(e.type == SDL_CONTROLLERSENSORUPDATE) {
                            es = e.csensor;
                            if(es.sensor == SDL_SENSOR_GYRO) {
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
        ArrayList<int[]> p = new ArrayList<>();
        //i feel like this might not be the cleverest way to error report, maybe i'm wrong though
        boolean on = true;
        int error = 0;
        int health = 20;
        strokeWeight(health/2);
        stroke(255);
        for(int i = 0;i < p.size();i ++) {
            int[] prev = p.get(i);
            if(i-1 >= 0) {
                prev = p.get(i-1);
            } else {
                if(!on) {
                    prev = p.get(p.size()-1);
                }
            }
            int[] curr = p.get(i);
            //x is 0, y is 1, pretty simple really
            line(curr[0], curr[1], prev[0], prev[1]);
        }
        circle(0, 0, 0);
        if(!on) {
            textSize(50);
            strokeWeight(1);
            text("FailPass".substring(error>20?0:4, 4 + (error>20?0:4)) + ": " + health + " health", 100, 100);
        }
        if(mousePressed) {
            drawing.create();
        }
        numSticksOld = numSticksNew;
    }
}