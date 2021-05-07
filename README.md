# Torch
Serverless messaging app for Android, intended for emergency situations, remote locations, and when cellular service is unavailable. 

## Installation
Clone this repository and import into Android Studio
```
git clone https://github.com/annalisetarhan/Torch.git
```

## Overview
Torch uses [Wi-Fi Aware](https://developer.android.com/guide/topics/connectivity/wifi-aware) to connect devices, forming clusters that can extend as far as the device links do. Each message is associated with exactly one hashtag, and the message is encrypted with a symmetric key derived from that hashtag. Once a pair or group of people agree on a hashtag for their conversation, their messages can be forwarded through other devices but only decrypted by each other. Users are only identified by their public key, which is used for private messages, so they can remain anonymous. 

On the first screen, users are asked to enter one or more hashtags to participate in, which become links to those hashtags’ conversations. Behind the scenes, when a new hashtag is entered, the app attempts to use that hashtag to decrypt each encrypted message in the database. Whenever the result isn’t gibberish, the decrypted message is displayed on the conversation screen. When a user deletes a hashtag (by long clicking it on the hashtags screen and pressing delete) the link to that hashtag’s conversation is removed, but previously decrypted messages remain decrypted in the database.

Messages are exchanged between devices in two ways. First, when a user sends a message, it is immediately forwarded to each device in the cluster it has previously communicated with. This is where the message size limit comes from, since these messages are limited to around 255 bytes, and much of it is used for metadata. The second is slower, but exhaustive. Once per minute, a runnable attached to a handler thread chooses a peer, opens a connection, and sends a copy of the entire database. This seemed a bit excessive at first, but a message id isn’t all that much smaller than an entire message, doing the exchange in multiple steps increases the complexity significantly, and databases should remain small. One reason the database will be small is because messages are ephemeral. Each is created with a specific time to die, which by default is 24 hours after it was sent. The same runnable that triggers periodic device connections also triggers a database purge, where any message whose time to die has passed is deleted. 

## What's Next
There is still quite a bit to do in order to make Torch fully functional, including testing it! All of the encryption has been tested and it works on the emulator, but the Wi-Fi Aware connection is (as far as I know) only testable on those rare devices that actually support it. Private messages are also mostly theoretical at this point. The code is there to encrypt and decrypt them, but there is no UI available to actually send or receive them. They also need a different type of message to be sent around, associating the truncated public keys that identify users with the full public key that can actually be used to send them private messages. The reason for using truncated public keys is how enormous RSA keys are. If they were sent around with the messages, the number of bytes available for the actual message would be cut in half. 

The other big thing that’s missing is a way for messages to propagate through the network quickly after they are sent. Right now, the message is only sent to the device’s own contacts, not forwarded, and only propagates further when devices open full connections. The difficulty, of course, is how to prevent infinite loops, where two (or more) devices pass a message around forever. This is probably just a matter of checking the database and only forwarding the message if it isn’t already present, but with the complications of inserting the message into the database asynchronously, that’s a project for another day.

## TODO
- User icons with images and colors derived from truncated public keys
- A tagging mechanism to label known users with names or nicknames
- A system to break up larger messages and reconstruct them at the receiver
- Ability to delete sent messages
- Setting to change default time to die on new messages
- A UI that isn’t exclusively Android defaults
