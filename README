The purpose of this repository is to show how the DigiOS SDK can be used to access and stream from the aux/tracking camera's.

Below is the API information

import com.digilens.digi_os_sdk.DigiOS_SDK;
int mode = DigiOS_SDK.ARGO_AUX_CAMERA_BOTH_ALLOW_ACCESS;
String package= "com.digilens.digios.sdk.test"
/*
Where Modes can be 
1. DigiOS_SDK.ARGO_AUX_CAMERA_ACCESS_DENIED 
2. DigiOS_SDK.ARGO_AUX_CAMERA_LEFT_ALLOW_ACCESS 
3. DigiOS_SDK.ARGO_AUX_CAMERA_RIGHT_ALLOW_ACCESS 
4. DigiOS_SDK.ARGO_AUX_CAMERA_BOTH_ALLOW_ACCES
*/
digios = new DigiOS_SDK();
digios.allowAuxCameraAccess(getApplicationContext(), mode , package);
// Code for Camera List request


Android Manifest Changes: API need the calling application below permissions.
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERACT_ACROSS_USERS" />

Notes:
1.	API can be called any number of times at runtime
2.	API must be called before requesting Camera List from Camera Service
3.	ARGO can stream only 2 camera feeds using ISP post processing.
4.	All three cameras can be streamed provided one/two cameras are working in RDI mode.
