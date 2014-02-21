Update 2014-02-09
-----------------
This repository has been transferred from https://github.com/sbstrm/appirater-android/ to https://github.com/drewjw81/appirater-android/ all of the old links and URLs now redirect here and no problems should occur, but please update to the new URL when possible.  All licenses have been transferred from sbstrm to me and the code is still released under MIT/X11.  Sorry for any inconvenience. 

Introduction
------------
Appirater is an android library based off the original Appirater By Arash Payan [Appirater iPhone] [appirater]. The goal is to 
create a cleanly designed App Rating prompt that you can drop into any android app that will help remind your users to 
review your app on the android Market.

![screenshot](https://raw.github.com/drewjw81/appirater-android/development/screenshot.png)

Like it's iPhone counterpart the code is released under the MIT/X11, so feel free to modify and share your changes with 
the world.

Getting Started (Eclipse)
-------------------------
1. Include the AppiraterAndroid library in your project.  Under your projects preferences -> Library section just click add and select the appirater-android library. 
2. Copy the /res/values/appirater-settings.xml from the AppiraterAndroid library in to your projects /res/values/ folder and adjust the settings to your preference.
3. Add Appirater.appLaunched(this); to the onCreate method in your main Activity.

Getting Started (Maven)
-----------------------
1. Install the library to you local repository using `mvn clean install`
2. Add the library as a dependency to your app:

	```
	<dependency>
	    <groupId>com.sbstrm</groupId>
	    <artifactId>appirater</artifactId>
	    <type>apklib</type>
	    <version>1.2</version>
	</dependency>
	```

3. Copy the /res/values/appirater-settings.xml from the AppiraterAndroid library in to your projects /res/values/ folder and adjust the settings to your preference.
4. Add Appirater.appLaunched(this); to the onCreate method in your main Activity.

Significant Events
------------------
Thanks to [Tarek Belkahia] [tokou] version 1.2 of AppiraterAndroid adds a Significant Event counter.  Set ```appirator_events_until_prompt``` in your appirater-settings.xml and call ```Appirater.significantEvent(context)``` each time a "Significant Event" occurs in your application.  Once the defined number of "Significant Events" have occurred the user will be prompted to rate the app next launch.

Upgrading to 1.1+
----------------
Users upgrading to 1.1+, please remove your old /res/values/settings.xml file from your application and follow step 2 under "Getting Started" above.

License
-------
Copyright 2011-2013 [sbstrm] [sbstrm].
Copyright 2014 [drewjw81] [drewjw81].
This library is distributed under the terms of the MIT/X11.

While not required, I greatly encourage and appreciate any improvements that you make
to this library be contributed back for the benefit of all who use Appirater.

Credits
-------
Orginal iPhone Appirater and translations By [Arash Payan] [arash]

Gradient button style by [Folkert Jongbloed] [folkert]

Also, thanks to [Chris Hager] [chrishager] who created AppRater for android

[appirater]: https://github.com/arashpayan/appirater/
[sbstrm]: http://sbstrm.co.jp
[drewjw81]: https://github.com/drewjw81
[arash]: http://arashpayan.com/
[folkert]: http://www.dibbus.com/2011/02/gradient-buttons-for-android/
[chrishager]: https://github.com/metachris/android-apprater
[tokou]: https://github.com/tokou
