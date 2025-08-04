import qupath.fx.dialogs.FileChoosers
import java.io.File

// =======================
// Prompt user to select output directory
// =======================
def defaultDir = new File(System.getProperty("user.home"))
def outputDirectory = FileChoosers.promptForDirectory("Please select a folder to save the GeoJSON annotation file", defaultDir)

// If the user cancels, stop the script
if (outputDirectory == null) {
    print "Export cancelled by user."
    return
}

// Get the name of the current image
def imageName = getCurrentImageData().getServer().getMetadata().getName()

// Construct the full output path
def outputPath = new File(outputDirectory, imageName + ".geojson").getAbsolutePath()

// Get the annotation objects
def annotations = getAnnotationObjects()

// Export the annotations to GeoJSON
exportObjectsToGeoJson(annotations, outputPath, "PRETTY_JSON", "FEATURE_COLLECTION")

print "Annotations saved to: " + outputPath
