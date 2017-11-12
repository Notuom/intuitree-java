# IntuiTree Java Library
This library is designed to generate JSON output from a flow process.

### Requirement
This library is using :
- Maven
- Java 1.8

### Overview

This library is designed as follows :
- Each time you create a log, an execution id will be set
- Each scenario of the execution ID is considered as a track
- A track begin with method "newTrack" and must be closed once by method "closeTrack"/"closeTrackSuccess"/"closeTrackFail"


### Sample Code

```java
// Initialisation
/// without id (will be generated)
IntuiLog log_without_id = new IntuiLog();

/// or with specific id
IntuiLog log_with_id = new IntuiLog("my-id");
```

```java
// Create a new track
log_with_id.newTrack("Description of the track");
```

```java
// Close a track with a successed end point
log_with_id.closeTrackFail("fail description");
// Close a track with a failed end point
log_with_id.closeTrackSuccess("success description");
```

```java
// Record content of an object in the log
log_with_id.append("the description of the object", complex_object);
```
