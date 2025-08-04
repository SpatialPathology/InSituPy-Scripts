/**
 * QuPath Export Script for InSituPy
 * --------------------------------
 * This script exports data from all named annotations in the current QuPath image.
 * It performs the following tasks:
 *
 * 1. Validates that all annotations are named and uniquely identified.
 * 2. Creates an export directory named after the current image inside the project folder.
 * 3. Exports:
 *    - All detected objects to a single GeoJSON file.
 *    - All measurements to a TSV file.
 *    - For each annotation:
 *        - A cropped image region as OME-TIFF.
 *        - The annotation geometry as GeoJSON.
 *        - All child objects (e.g., cells) as GeoJSON.
 *        - Measurements of child objects to a TSV file.
 *
 * Requirements:
 *  - The script must be run within a QuPath project.
 *  - All annotations must be named and have unique names.
 *  - The image must be open and selected in QuPath.
 *
 * Output:
 *  - A folder named after the image will be created inside a subfolder called "insitupy" in the project directory.
 *  - Each annotation will have its own subfolder containing image data, geometry, and measurements.
 *
 * Notes:
 *  - RGB images will be exported with interleaved channels.
 *  - Multiplexed images will be exported with separate grayscale channels.
 *  - Compression is set to ZLIB for OME-TIFF files.
 *
 * Example folder structure:
 *  project/
 *    └── insitupy/
 *        └── ImageName/
 *            ├── cells.geojson
 *            ├── measurements.tsv
 *            ├── Annotation1/
 *            │   ├── image.ome.tif
 *            │   ├── annotation.geojson
 *            │   ├── cells.geojson
 *            │   └── measurements.tsv
 *            └── Annotation2/
 *                ├── ...
 */


import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject
import qupath.lib.images.writers.ome.OMEPyramidWriter

// Get the current project and its base directory
def project = getProject()
def baseDir = project.getBaseDirectory().getAbsolutePath()
def currentImage = getCurrentImageData()
def projectEntry = project.getEntry(currentImage)

if (projectEntry == null) {
    print "Error: Could not find project entry for the current image."
    return
}

// Get annotations and check for unique names
def annotations = getAnnotationObjects()

def unnamed = annotations.findAll { !it.getName() }
if (!unnamed.isEmpty()) {
    throw new RuntimeException("All annotations must be named. Please name all annotations before exporting.")
}

def names = annotations.collect { it.getName() ?: "unnamed" }
def nameSet = new HashSet()
def duplicates = names.findAll { !nameSet.add(it) }.unique()

if (!duplicates.isEmpty()) {
    println "ERROR: Duplicate annotation names found:"
    duplicates.each { println " - '${it}' appears more than once" }
    throw new RuntimeException("Annotation names must be unique. Please rename duplicates and try again.")
}

// Get the image name and sanitize it for folder naming
def imageName = currentImage.getServer().getMetadata().getName().replaceAll("[^a-zA-Z0-9-_\\.]", "_")

// Create export directory named after the image
def outDir = new File(baseDir, "insitupy")
def exportDir = new File(outDir, imageName)
if (!exportDir.exists()) {
    exportDir.mkdirs()
}

// === Export all objects to GeoJSON ===
def geoJsonPath = new File(exportDir, "cells.geojson").getAbsolutePath()
exportAllObjectsToGeoJson(geoJsonPath, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")

// === Export measurements for the current image only ===
def separator = "\t"
def exportType = PathCellObject.class
def outputPath = new File(exportDir, "measurements.tsv")

def exporter = new MeasurementExporter()
    .imageList([projectEntry])
    .separator(separator)
    .exportType(exportType)
    .exportMeasurements(outputPath)

print "Measurement export complete! File saved in: " + outputPath.getAbsolutePath()

// === Export images and data for all annotations ===
def file_suffix = ".ome.tif"
def tilesize = 1024
def outputDownsample = 1
def pyramidscaling = 2
def compression = OMEPyramidWriter.CompressionType.ZLIB
double downsample = 1.0

def server = getCurrentServer()

for (def annotation in annotations) {
    def annotIndex = annotations.indexOf(annotation)
    def annot_name = annotation.getName()
    def roi = annotation.getROI()
    println "Exporting annotation ${annotIndex + 1} of ${annotations.size()} with area: ${roi.getArea()}"

    int height = roi.getBoundsHeight()
    int width = roi.getBoundsWidth()
    int x = roi.getBoundsX()
    int y = roi.getBoundsY()

    // Sanitize annotation name for file safety
    def safeName = annot_name.replaceAll("[^a-zA-Z0-9-_\\.]", "_")

    // Create a subdirectory for this annotation
    def annotDir = new File(exportDir, safeName)
    if (!annotDir.exists()) {
        annotDir.mkdirs()
    }

    // === Export image region ===
    
    // === Check if image is RGB ===
    def isRGB = server.getMetadata().isRGB()
    
    // === Define output image name and path ===
    def outputImageName = "image${file_suffix}"
    def imagePath = new File(annotDir, outputImageName)
    
    // === Build OME-TIFF writer ===
    def writerBuilder = new OMEPyramidWriter.Builder(server)
        .region(x, y, width, height)
        .compression(compression)
        .parallelize()
        .tileSize(tilesize)
        .scaledDownsampling(outputDownsample, pyramidscaling)
    
    // === Configure channel layout based on image type ===
    if (isRGB) {
        writerBuilder.channelsInterleaved()
        print "Exporting as RGB with interleaved channels."
    } else {
        // Do not call channelsInterleaved() — default is separate channels
        print "Exporting as multiplexed grayscale with separate channels."
    }
    
    // === Write the image ===
    writerBuilder.build().writePyramid(imagePath.getAbsolutePath())


    // === Export objects within annotation ===
    selectObjects([annotation])
    def annotGeoJsonPath = new File(annotDir, "annotation.geojson").getAbsolutePath()
    exportObjectsToGeoJson([annotation], annotGeoJsonPath, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")
    
    def hierarchy = getCurrentHierarchy()
    def children = hierarchy.getObjects(null, null).findAll { it.getParent() == annotation }
    def childGeoJsonPath = new File(annotDir, "cells.geojson").getAbsolutePath()
    exportObjectsToGeoJson(children, childGeoJsonPath, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")

    // === Export measurements of child objects ===
    def annotMeasurementPath = new File(annotDir, "measurements.tsv")
    def exporterAnnot = new MeasurementExporter()
        .imageList([projectEntry])
        .separator(separator)
        .exportType(exportType)
        .filter(obj -> obj.getParent()?.getName() == annotation.getName())
        .exportMeasurements(annotMeasurementPath)
}

print "All exports complete!"

println "\nExport Summary:"
println "- GeoJSON: ${geoJsonPath}"
println "- Measurements: ${outputPath.getAbsolutePath()}"
println "- Annotation images and data: ${annotations.size()} exported to ${exportDir.getAbsolutePath()}"
