package com.mmu;

import org.libsdl.api.joystick.SDL_Joystick;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_JOYSTICK;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.Sdl.SDL_Quit;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.joystick.SdlJoystick.SDL_JoystickClose;
import static org.libsdl.api.joystick.SdlJoystick.SDL_JoystickOpen;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;

public class Main {

    public static void main(String[] args) {
        if((SDL_Init(SDL_INIT_JOYSTICK) == -1)) {
            System.out.println(SDL_GetError());
        }
        //Game Controller 1 handler
        SDL_Joystick gGameController;
        //Check for joysticks
        if( SDL_NumJoysticks() < 1 )
        {
            System.out.println( "Warning: No joysticks connected!\n" );
        }
        else
        {
            System.out.println(SDL_NumJoysticks());
            //Load joystick
            gGameController = SDL_JoystickOpen( 0 );
            if( gGameController == null )
            {
                System.out.println( "Warning: Unable to open game controller! SDL Error: %s\n" + SDL_GetError() );
            }
            SDL_JoystickClose( gGameController );
        }
        SDL_Quit();
    }
}
