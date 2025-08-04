/**
 * QuPath InstanSeg Batch Annotation Script
 * ----------------------------------------
 * This script runs InstanSeg segmentation on all annotations in the current QuPath image.
 * It allows the user to:
 *  - Select the InstanSeg model directory interactively.
 *  - Select a `.txt` file containing channel names (one per line).
 *  - Validate that all channel names exist in the current image.
 *  - Skip annotations that are empty or invalid.
 *  - Print progress for each annotation.
 *  - Summarize how many annotations were successfully segmented.
 *
 * Requirements:
 *  - The `.txt` file should contain one channel name per line.
 *  - All channel names must match those in the current image.
 *  - A QuPath project should be open to default the channel file location.
 *
 * Example channel list file:
 * ---------------------------
 * DAPI-01
 * aSMA
 * CD34
 * CD31
 * CD163
 * CD20
 * CD14
 * Pan-Cytokeratin
 * CD45
 * HLA-DR
 * CD66b
 * CD3e
 */

import qupath.fx.dialogs.FileChoosers
import qupath.lib.common.GeneralTools
import qupath.lib.images.servers.ColorTransforms
import javafx.stage.FileChooser
import qupath.lib.gui.QuPathGUI

// Define default directory inside user's home directory
def userHome = System.getProperty("user.home")
def defaultModelDir = new File(userHome, "QuPath/InstanSeg/downloaded")

// Prompt user to select the model directory
def modelDir = FileChoosers.promptForDirectory("Please select the InstanSeg model folder", defaultModelDir)
if (modelDir == null) {
    print "Model selection cancelled."
    return
}

// Prompt user to select the channel list file
// Get the current QuPath GUI window
def window = QuPathGUI.getInstance().getStage()

// Get the project directory or fallback to user home
def project = getProject()
def defaultChannelDir = project == null ? new File(System.getProperty("user.home")) : project.getBaseDirectory()

// Prompt for the channel file
def channelFile = FileChoosers.promptForFile(
    window,
    "Select the TXT file with channel names",
    defaultChannelDir,
    [new FileChooser.ExtensionFilter("Text files", "*.txt")] as FileChooser.ExtensionFilter[]
)

if (channelFile == null || !channelFile.exists()) {
    print "Channel file not selected or doesn't exist."
    return
}

// Read channel names from file
def channelNames = channelFile.readLines().collect { it.trim() }.findAll { !it.isEmpty() }

// Get available channel names in the current image
def imageData = getCurrentImageData()
def availableChannels = imageData.getServer().getMetadata().getChannels().collect { it.getName() }

// Validate channel names
def invalidChannels = channelNames.findAll { !availableChannels.contains(it) }
if (!invalidChannels.isEmpty()) {
    print "The following channels are not found in the current image: " + invalidChannels
    return
}

// Print the valid channels that will be used
print "Channels parsed from file and validated for segmentation:"
channelNames.each { println "- ${it}" }

// Create channel extractors
def channelExtractors = channelNames.collect { ColorTransforms.createChannelExtractor(it) }

// Get annotations
def annotations = getAnnotationObjects()

// Loop through each annotation and run InstanSeg
int total = annotations.size()
int index = 1
int successful = 0

for (annotation in annotations) {
    def roi = annotation.getROI()
    if (roi == null || roi.getArea() == 0) {
        print "Skipping empty or invalid annotation ${index} of ${total}"
        index++
        continue
    }

    print "Processing annotation ${index} of ${total}"
    selectObjects([annotation])

    qupath.ext.instanseg.core.InstanSeg.builder()
        .modelPath(modelDir.getAbsolutePath())
        .device("cpu")
        .inputChannels(channelExtractors)
        .outputChannels()
        .tileDims(512)
        .interTilePadding(48)
        .nThreads(4)
        .makeMeasurements(true)
        .randomColors(true)
        .outputType("Cells")
        .build()
        .detectObjects()

    successful++
    index++
}

print "Segmentation completed. ${successful} of ${total} annotations were successfully processed."
