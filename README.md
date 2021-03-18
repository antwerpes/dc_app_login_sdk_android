DocCheck App Login SDK Android 
==============================

The DocCheck App Login SDK provides you with a simple to use integration of the authentication through DocCheck. 
This is done by providing an Activity which wraps the Web flow and handles callbacks for the authentication.

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
