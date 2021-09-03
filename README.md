# Get Food Recommendation with Wit.ai

## Overview

In this tutorial, we will be creating a voice-enabled Android app that greets the user. The app will be able to process the user's voice response and respond to the user appropriately. The key things we will explore is how to:

*   Design the user interaction
*   Create and train a Wit app to do natural language processing (NLP)
*   Integrate Wit with your Android app



## Prerequisites

*   Create a [Wit.ai](https://wit.ai/) account
*   Download the [Wit.ai Voice demo base-setup](https://github.com/wit-ai/android-voice-demo/tree/base-setup) branch from GitHub
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
