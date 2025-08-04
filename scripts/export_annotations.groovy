/**
 * QuPath Annotation GeoJSON Export Script
 * ---------------------------------------
 * This script exports all annotation objects from the currently opened image in QuPath
 * to a single GeoJSON file. The user is prompted to select an output directory, and the
 * file is named after the image.
 *
 * Features:
 *  - Prompts the user to select a folder for saving the output.
 *  - Automatically names the output file using the current image name.
 *  - Exports all annotations in GeoJSON format using the "PRETTY_JSON" and "FEATURE_COLLECTION" options.
 *
 * Requirements:
 *  - An image must be open in QuPath.
 *  - The image must contain annotation objects.
 *
 * Output:
 *  - A `.geojson` file containing all annotations will be saved in the selected folder.
 *
 * Example output filename:
 *  - ImageName.geojson
 *
 * Notes:
 *  - If the user cancels the folder selection, the script will exit without exporting.
 */


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
