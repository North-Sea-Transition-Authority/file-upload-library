# File upload library

[![Build Status](https://drone-github.fivium.co.uk/api/badges/Fivium/file-upload-library/status.svg?ref=refs/heads/main)](https://drone-github.fivium.co.uk/Fivium/file-upload-library)

## [Documentation](./docs)

## How to use this starter in your project

You will need to add the following to your `build.gradle` `dependecies`

```gradle
implementation "uk.co.fivium:sprint-boot-file-upload-starter:version"
```

## Running the [example project](./example)

> **_Make sure you are using node 14. This is a requirement for FDS._**

To get Node 14 you can do the following

### On Windows

Go to node [previous releases](https://nodejs.org/en/download/releases)

- Download the latest version of Node 14
- Install it
- Update your node path to wherever you installed node 14
- In your terminal, run `node -v` and confirm it returns 14.x.x

### Or if you have homebrew
```bash
brew install node@14
brew unlink node
brew link node@14
node -v # Confirm it returns 14.x.x
```

Then start the project by running the following:
```bash
chmod +x ./example/start.sh
./example/start.sh
```
