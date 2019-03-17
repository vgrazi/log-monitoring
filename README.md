# log-monitoring
Explanation of architecture:
`Method: fileReader.tailFile(recordQueue)`  
The FileReader parses records into Record instances, then deposits them on the queue.

`recordProcessor.processRecords(recordQueue, frameQueue)`  
As records appear, RecordProcessor processes them, groups them into Frames, and deposits the Frames onto the frameQueue

`frameProcessor.processFrames(frameQueue, scorecardQueue)`  
FrameProcessor grabs frames, forms them into "Windows" (10 minutes spans), and produces Scorecards


`scorecardProcessor.processScorecard(scorecardQueue)`  
ScorecardProcessor grabs new scorecards and writes them to the file system. Also cleans up old files

Java UI Application watches for new files, reads them, and displays them
