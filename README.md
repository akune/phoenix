# Phoenix - Browser-Based Messaging With End-To-End Encryption

This project aims to provide a GWT-based browser chat client that allows participants to communicate through REST services with end-to-end encryption. 

## Client Start-Up
1. Initialize cipher suite
 1. Try to obtain a key pair from request parameters
 2. If the previous step failed try to obtain a key pair from local storage
 3. If the previous step failed generate a new key pair
2. Initialize identity
 1. Try to obtain identity from local storage or generate a new one
 2. TODO: Try to obtain identity from identity server based on public key hash. If no identity could be found at the identity server send the one stored / generated locally. Otherwise update the locally stored identity with the one received from the identity server.
3. Set-up a client session
 1. Register handlers for conversation initiations, connection changes and server identification changes
 2. Start the messaging service 
