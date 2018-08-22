; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "NUS IVLe Desktop Synchroniser"
#define MyAppVersion "1.3"
#define MyAppPublisher "Quan Le, Inc."
#define MyAppURL "https://github.com/quanle1994/IVLE-Desktop-Synchroniser"
#define MyAppExeName "IVLeDesktopSync.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{3C78D28A-E223-41D6-AD4B-7623C9E0D2E4}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\IVLeDesktopSync
DisableProgramGroupPage=yes
OutputDir=E:\School\SummerProjects\IVLE-Desktop-Synchroniser\out\artifacts\IVLESync_jar
OutputBaseFilename=IVLeDesktopSyncSetup
SetupIconFile=E:\School\SummerProjects\IVLE-Desktop-Synchroniser\src\java\main\resources\address_book_32.ico
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "E:\School\SummerProjects\IVLE-Desktop-Synchroniser\out\artifacts\IVLESync_jar\windowsInstaller\IVLeDesktopSync.exe"; DestDir: "{app}"
Source: "E:\School\SummerProjects\IVLE-Desktop-Synchroniser\out\artifacts\IVLESync_jar\windowsInstaller\JRE\*"; DestDir: "{app}\JRE"; Flags: recursesubdirs createallsubdirs


[Icons]
Name: "{commonprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

