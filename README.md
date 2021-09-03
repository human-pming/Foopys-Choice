# Foopy's Choice

Weather-based food recommendation service
(2021.08.03~2021.09.03)

https://user-images.githubusercontent.com/77844152/131990939-dd0d5262-b049-4c74-99c8-30d718669a32.png
## Overview

In this project, we will be creating a voice-enabled Android app that greets the user. The app will be able to process the user's voice response and respond to the user appropriately. The key things we will explore is how to:

*   Design the user interaction
*   Create and train a Wit app to do natural language processing (NLP) to catogorize weather
*   Integrate Wit with Android app
*   Randomly recommend recipes based on weather

## Prerequisites  

You can download this applicaiton in google play store(to be modified)

If you want to modify the files or download the files
*   Clone this repository [Foopy's Choice](https://github.com/guen-a-park/Foopys-Choice.git) or Download Zip
*   Download and install [Android Studio](https://developer.android.com/studio)
*   Have an Android device/emulator with
    *   Internet access
    *   Microphone access
    *   API Level 26 or greater
    > If you are using a device [enable USB debugging](https://developer.android.com/studio/debug/dev-options)

## Conversation Design

When designing applications with voice interactions, it's important to understand the various ways that a user may interact with your app. Some techniques that can help with modeling the conversation is writing a script or creating a flow diagram. For our greeting app, let's write a script to outline it.

Let's consider the following conversation as the happy path:
```
Wit:  "Hi, welcome to the Wit.ai voice demo. I'm Wit. What is your name?"

User: "My name is Pan"

Wit:  "Nice to meet you Pan!"
```

Now let's think about scenarios were the user can deviate:
```
Wit:  "Hi, welcome to the Wit.ai speech demo. I'm Wit. What is your name?"

User: "I want pizza"

Wit:  "Sorry, I didn't get that. What is your name?"

User: "My name is Pan"

Wit:  "Nice to meet you Pan!"
```


## Reference
https://github.com/haerulmuttaqin/FoodsApp-starting-code
https://github.com/wit-ai/android-voice-demo
https://wit.ai/
https://www.themealdb.com/


