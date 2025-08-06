/**
 * QuPath InstanSeg Batch Script
 * ----------------------------------------
 * This script runs InstanSeg segmentation on all annotations in the current QuPath image.
 * It is optimized for batch processing across multiple images using "Run for project".
 *
 * Features:
 *  - Prompts the user to select the InstanSeg model directory and a `.txt` file with channel names.
 *  - Validates that all channel names exist in the current image.
 *  - Skips annotations that are empty or invalid.
 *  - Prints progress for each annotation.
 *  - Summarizes how many annotations were successfully segmented.
 *  - Stores the selected model path and channel file in a temporary settings file.
 *  - Reuses these settings automatically for subsequent images in the batch.
 *  - Only prompts again if more than 10 seconds have passed since the last settings save.
 *
 * Requirements:
 *  - The `.txt` file should contain one channel name per line.
 *  - All channel names must match those in the current image.
 *  - A QuPath project should be open to store the temporary settings file.
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
import qupath.lib.images.servers.ColorTransforms
import javafx.stage.FileChooser
import qupath.lib.gui.QuPathGUI

// Define settings file location
def project = getProject()
def settingsFile = project == null
    ? new File(System.getProperty("user.home"), "instanseg_settings.txt")
    : new File(project.getBaseDirectory(), "instanseg_settings.txt")

def modelDir
def channelFile
def now = System.currentTimeMillis()
def timestampValid = false

// Load settings if file exists and check timestamp
if (settingsFile.exists()) {
    def lines = settingsFile.readLines().findAll { !it.trim().isEmpty() }
    if (lines.size() >= 3) {
        modelDir = new File(lines[0].trim())
        channelFile = new File(lines[1].trim())
        def timestamp = lines[2].trim().toLong()
        def timeDifference = now - timestamp
        println "Time since last settings save: ${timeDifference} ms"
        if (timeDifference <= 5000) {
            timestampValid = true
        }
    }
}


// Prompt if settings not loaded or timestamp expired
if (!timestampValid || modelDir == null || !modelDir.exists()) {
    def defaultModelDir = new File(System.getProperty("user.home"), "QuPath/InstanSeg/downloaded")
    modelDir = FileChoosers.promptForDirectory("Please select the InstanSeg model folder", defaultModelDir)
    if (modelDir == null) {
        print "Model selection cancelled."
        return
    }
}

if (!timestampValid || channelFile == null || !channelFile.exists()) {
    def window = QuPathGUI.getInstance().getStage()
    def defaultChannelDir = project == null ? new File(System.getProperty("user.home")) : project.getBaseDirectory()
    channelFile = FileChoosers.promptForFile(
        window,
        "Select the TXT file with channel names",
        defaultChannelDir,
        [new FileChooser.ExtensionFilter("Text files", "*.txt")] as FileChooser.ExtensionFilter[]
    )
    if (channelFile == null || !channelFile.exists()) {
        print "Channel file not selected or doesn't exist."
        return
    }
}

// Read and validate channel names
def channelNames = channelFile.readLines().collect { it.trim() }.findAll { !it.isEmpty() }
def imageData = getCurrentImageData()
def availableChannels = imageData.getServer().getMetadata().getChannels().collect { it.getName() }

def invalidChannels = channelNames.findAll { !availableChannels.contains(it) }
if (!invalidChannels.isEmpty()) {
    print "The following channels are not found in the current image: " + invalidChannels
    return
}

print "Channels parsed from file and validated for segmentation:"
channelNames.each { println "- ${it}" }

def channelExtractors = channelNames.collect { ColorTransforms.createChannelExtractor(it) }
def annotations = getAnnotationObjects()
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

// Save settings with timestamp
def end_time = System.currentTimeMillis()
settingsFile.text = modelDir.getAbsolutePath() + "\n" +
                    channelFile.getAbsolutePath() + "\n" +
                    end_time.toString()

print "Segmentation completed. ${successful} of ${total} annotations were successfully processed."
