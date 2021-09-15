DocCheck App Login SDK Android 
==============================

The DocCheck App Login SDK provides you with a simple to use integration of the authentication through DocCheck. 
This is done by providing an Activity which wraps the Web flow and handles callbacks for the authentication.

For more details regarding the DocCheck login product and licenses visit [our website](https://more.doccheck.com/en/industry/) or request initial information via app.industry@doccheck.com.

## Requirements

### Android

- Min SDK Version 26
- Kotlin Enabled
- AndroidX enabled

### DocCheck Login ID
In order to ensure smooth integration and functioning always adhere to using the respective framework. Before you can implement the DocCheck Login in your app, you will need to set up a new login in CReaM (http://crm.doccheck.com/com/). For detailed instructions please read the technical manual in the download section of https://more.doccheck.com/en/industry/ (chapter 2.2.3). When a new login has been created, please add the bundle identifier (for iOS) or the package name (for Android) in the destination URL. This process in general runs as follows: topleveldomain.companyname.appname 

Example for input in the target URL in CReaM: 
```shell
doccheck://login?appid=bundleidentifier
```
### DocCheck License
For mobile applications a mobile license is required.To get more details about the different packages (basic, economy and business) as well as booking process please contact app.industry@doccheck.com. 

Installation
------------

TODO: Depending on maven publication or github release

// maven

The latest release is published on [MavenCentral](https://mvnrepository.com/artifact/com.doccheck.apploginsdk/apploginsdk).

A simple integration with your project in Android Studio using gradle would look like this.

`build.gradle`:

```groovy
dependencies {
	implementation("com.doccheck.apploginsdk:apploginsdk:0.1.0")
}
```

// github

Grab the latest release from [here](https://github.com/ORGANISATION_PLACEHOLDER/REPOSITORY_PLACEHOLDER/releases) and place it in your Android project for example in a libs folder.

By adding the dependency task on that folder the SDK will be available in your App.

`build.gradle`:

```groovy
dependencies {
	implementation fileTree(dir: 'libs', includes: ['*.aar'])
}
```

Dependencies
------------
// needed for github, maven has those given as transitive or required

The SDK relies on common dependencies provided to be compatible with older versions of Android and to use common functionalities like Coroutine for background task. 

````groovy
dependencies {
    implementation 'androidx.core:core-ktx:1.3.2'
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2'
}
````

Usage
-----

Integrating and using the SDK requires you to have a valid configuration setup in the [management portal ](LINK HERE). Once that is done just provide the login id and start the activity to get the authentication flow started.


### Start the DocCheckLoginActivity

````kotlin
val intent = Intent(it.context, DocCheckLoginActivity::class.java).apply {
    putExtra("loginId", LOGIN_ID)
    putExtra("language", SELECTED_LANGUAGE)
    putExtra("templateName", TEMPLATE_NAME) // Optional will default to s_mobile
}

startActivityForResult(intent, YOUR_REQUEST_CODE)
````

### End of login flow

The login flow can either be canceled, succeeded or failed and will respond accordingly. 

The successful end of the flow will be indicated with the Android result code `Activity.RESULT_OK` and a canceled or failed login flow will return the `Activity.RESULT_CANCELED`. Additionally to that the result intent will be propagated with some values. 

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != YOUR_REQUEST_CODE) {
        return // not the result your looking for because of different result code
    }
    
    val loginResult = data?.getBooleanExtra("LOGIN_RESULT", false) // indicates if the login flow was successful or not additionally to the result code
    val loginResponse = data?.getStringExtra("RESPONSE") // indicates the reason for ending the login flow. possible values: CANCEL, ERROR, SUCCEEDED
    val map = data?.getSerializableExtra("URLPARAMS") as? Map<String, List<String>?> // values provided by the authentication flow on success if available
}
```

Compiling
---------
Executing the gradle task `applogin:build` will build the library and places the finished AAR files in the `applogin/build/outputs/aar` folder.


## Example

An example project with integration instructions can be found in the [Example Repository](https://github.com/antwerpes/dc_app_login_sdk_android_example)



## Response Parameters


| Name           |Status   |Description                                                                | Value                                                               | License Type     |
|----------------|---------|---------------------------------------------------------------------------|---------------------------------------------------------------------|------------------|
|login_id        |internal |login ID associated with the login                                         |e.g. 200000012345                                                    |all               |
|appid           |internal |bundle identifier for the current app, is related to the mobile special    |e.g. "bundleidentifier"                                              |all   		    |
|intdclanguageid |internal |internal ID that tracks the user language                                  |e.g. 148                                                             |all               |
|strDcLanguage   |internal |iso code that tracks the user language                                     |(for Personal form). One of "de", "en"/"com", "fr", "nl", "it", "es".|all               |
|uniquekey       |valid    |alphanumerical string that is individual per user, is passed by each login |e.g. abc_abc884e739adf439ed521720acb5b232                            |economy + business|
|code            |valid    |Oauth2 parameter                                                           |e.g. abc884e739adf439ed521720acb5b232abc884e739adf439ed521720acb5b232|economy + business|
|state           |valid    |Oauth2 parameter                                                           |e.g. eHxI902CC3doao1                                                 |economy + business|
|dc_agreement    |valid    |status of confirmation of the data transfer consent form                   |0 = not confirmed; 1 = confirmed                                     |business          |

Please note that additional parameters can be delivered in case of valid consent for data transfer in combination with an implemented business license. For more Details, please check the OAuth2 documentation. Thats one can be reuqested via app.industry@doccheck.com. 

