# About the Librelio Magazine Solution
The Librelio Magazine Solution is intended mainly for magazine publishers. It consists in:
- customizable mobile apps: a complete version has been developed on iOS, and is under development on Android (here) and [Windows 8](https://github.com/Librelio/windows8)
- a cloud based server

Examples of apps developed using this solution are listed on www.librelio.com . A good example of iOS app is the [Wind magazine app](https://itunes.apple.com/fr/app/wind-magazine/id433594605?mt=8). 

# Purpose of this project
This project aims at porting the existing iOS customizable app to Android. 

# Customization of the app
## 1- Replace the following elements with your cusstom elements:
*  assets directory
*  in res directory, all subdirectories ending with -port or -land
*  in res/values directory, application_.xml

## 2- Rename the package in Eclipse:
As explained on [Stack Overflow](http://stackoverflow.com/questions/3697899/package-renaming-in-eclipse-android-project), press F2 on package name. 
![RenamePackage](RenamePackage.PNG)


## 3- Update AndroidManifest.xml:
Enter the new package name.
