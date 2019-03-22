# log-monitoring
Explanation of architecture:
`Method: fileReader.tailFile(recordQueue)`  
The FileReader parses records into Record instances, then deposits them on the queue.

`recordProcessor.processRecords(recordQueue, frameQueue)`  
As records appear, RecordProcessor processes them, groups them into Frames, and deposits the Frames onto the frameQueue

`frameProcessor.processFrames(frameQueue, scorecardQueue)`  
FrameProcessor grabs frames, forms them into "Windows" (10 minutes spans), and produces Scorecards.
A Scorecard contains all of the information needed to render the client. All of the generations are contained in the 
Scorecard. By Default, the scorecard retains 10 minutes of summary data, including hit counts, alerts, history, 
and alert state.


`scorecardProcessor.processScorecard(scorecardQueue)`  
ScorecardProcessor grabs new scorecards and writes them to the file system. Also cleans up old files

Java UI Application watches for new Scorecard files, reads them, and renders them

Note: To run this in diagnostic mode (timings are contracted, use application parameter --spring.profiles.active=dev)
