package com.mmu;

import org.libsdl.api.gamecontroller.SDL_GameController;

import static org.libsdl.api.SDL_SubSystem.SDL_INIT_GAMECONTROLLER;
import static org.libsdl.api.Sdl.SDL_Init;
import static org.libsdl.api.Sdl.SDL_Quit;
import static org.libsdl.api.error.SdlError.SDL_GetError;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerClose;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_GameControllerOpen;
import static org.libsdl.api.gamecontroller.SdlGamecontroller.SDL_IsGameController;
import static org.libsdl.api.joystick.SdlJoystick.SDL_NumJoysticks;

public class Main {

    public static void main(String[] args) {
        if((SDL_Init(SDL_INIT_GAMECONTROLLER) == -1)) {
            System.out.println(SDL_GetError());
        }
        //Check for joysticks
        if( SDL_NumJoysticks() < 1 )
        {
            System.out.println( "Warning: No joysticks connected!\n" );
        }
        else
        {
            SDL_GameController DS5 = null;
            for(int i = 0; i < SDL_NumJoysticks(); i++)
            {
                if(SDL_IsGameController(i))
                {
                    //Load DS5
                    DS5 = SDL_GameControllerOpen(i);
                }
            }
            if( DS5 == null )
            {
                System.out.println( "Warning: Unable to open game controller! SDL Error: %s\n" + SDL_GetError() );
            } else {
                SDL_GameControllerClose( DS5 );
            }
        }
        SDL_Quit();
    }
}
