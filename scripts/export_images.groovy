/**
 * QuPath script to export OME-TIFF images for each annotation bounding box.
 * Compatible with QuPath v0.4.3+
 */

import qupath.lib.images.writers.ome.OMEPyramidWriter
import java.io.File

// =======================
// Configuration
// =======================
def fileSuffix = ".ome.tif"
def tileSize = 1024
def outputDownsample = 1
def pyramidScaling = 2
def compression = OMEPyramidWriter.CompressionType.ZLIB
double downsample = 1.0

// =======================
// Prepare export directory
// =======================
def exportDir = buildPathInProject("image_export")
mkdirs(exportDir)
println "Export directory: " + exportDir

// =======================
// Get image and annotations
// =======================
def server = getCurrentServer()
def imageName = getCurrentImageNameWithoutExtension()
def annotations = getAnnotationObjects()

if (annotations.isEmpty()) {
    println "No annotations found. Exiting."
    return
}

println "Found ${annotations.size()} annotations. Starting export..."

// =======================
// Export each annotation
// =======================
annotations.eachWithIndex { annotation, index ->
    def annotName = annotation.getName()
    if (!annotName) {
        println "WARNING: Annotation ${index + 1} has no name. Skipping."
        return
    }

    def roi = annotation.getROI()
    def area = roi.getArea()
    int x = (int) roi.getBoundsX()
    int y = (int) roi.getBoundsY()
    int width = (int) roi.getBoundsWidth()
    int height = (int) roi.getBoundsHeight()

    println "Exporting annotation ${index + 1}/${annotations.size()} — '${annotName}'"
    println "  Area: ${area}, Bounds: (${x}, ${y}, ${width}, ${height})"

    // === Export image region ===
    def isRGB = server.getMetadata().isRGB()
    def safeName = annotName.replaceAll("[^a-zA-Z0-9-_\\.]", "_")
    def outputImageName = "${safeName}${fileSuffix}"
    def imagePath = new File(exportDir, outputImageName)

    def writerBuilder = new OMEPyramidWriter.Builder(server)
        .region(x, y, width, height)
        .compression(compression)
        .parallelize()
        .tileSize(tileSize)
        .scaledDownsampling(outputDownsample, pyramidScaling)

    if (isRGB) {
        writerBuilder.channelsInterleaved()
        println "  Exporting as RGB with interleaved channels."
    } else {
        println "  Exporting as multiplexed grayscale with separate channels."
    }

    writerBuilder.build().writePyramid(imagePath.getAbsolutePath())
    println "  Saved to: ${imagePath.getAbsolutePath()}"
}

println "✅ All annotation exports complete!"
