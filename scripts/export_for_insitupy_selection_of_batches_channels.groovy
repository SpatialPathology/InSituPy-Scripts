import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject
import qupath.lib.images.writers.ome.OMEPyramidWriter

// === Konfiguration ===
def channelListFile = new File("/Volumes/KINGSTON/CODEX/Qupath/channels/channels.txt") 
def tilesize = 1024
def outputDownsample = 1
def pyramidscaling = 2
def compression = OMEPyramidWriter.CompressionType.ZLIB
def batchSize = 100

// === Projekt-Setup ===
def project = getProject()
def baseDir = project.getBaseDirectory().getAbsolutePath()
def currentImage = getCurrentImageData()
def projectEntry = project.getEntry(currentImage)
if (projectEntry == null) {
    print "Error: Could not find project entry for the current image."
    return
}
def server = getCurrentServer()

// === Kanalnamen aus TXT einlesen ===
if (!channelListFile.exists()) {
    throw new RuntimeException("Channel list file not found: " + channelListFile)
}
def channelNamesToExport = channelListFile.readLines()
    .collect { it.trim() }
    .findAll { !it.isEmpty() }

println "Kanäle laut TXT: " + channelNamesToExport

// Verfügbare Kanäle im Bild
def availableChannelNames = server.getMetadata().getChannels().collect { it.getName() }
println "Verfügbare Kanäle im Bild: " + availableChannelNames

// Namen → Indizes umwandeln
def channelIndices = channelNamesToExport.collect { name ->
    def idx = availableChannelNames.indexOf(name)
    if (idx < 0) {
        println "WARNUNG: Kanal '${name}' nicht im Bild gefunden."
    }
    return idx
}.findAll { it >= 0 }

if (channelIndices.isEmpty()) {
    throw new RuntimeException("Keine der angegebenen Kanäle wurden gefunden!")
}
println "Exportiere Kanal-Indices: " + channelIndices

// === Annotationen laden und prüfen ===
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

// === Export-Ordner erstellen ===
def imageName = currentImage.getServer().getMetadata().getName().replaceAll("[^a-zA-Z0-9-_\\.]", "_")
def outDir = new File(baseDir, "insitupy")
def exportDir = new File(outDir, imageName)
if (!exportDir.exists()) {
    exportDir.mkdirs()
}

for (def annotation in annotations) {
    def annotIndex = annotations.indexOf(annotation)
    def annot_name = annotation.getName()
    def roi = annotation.getROI()
    println "Exporting annotation ${annotIndex + 1} of ${annotations.size()} with area: ${roi.getArea()}"

    int height = roi.getBoundsHeight()
    int width = roi.getBoundsWidth()
    int x = roi.getBoundsX()
    int y = roi.getBoundsY()

    def safeName = annot_name.replaceAll("[^a-zA-Z0-9-_\\.]", "_")
    def annotDir = new File(exportDir, safeName)
    if (!annotDir.exists()) {
        annotDir.mkdirs()
    }

    // === OME-TIFF Export nur mit gefilterten Kanälen ===
    def outputImageName = "image.ome.tif"
    def imagePath = new File(annotDir, outputImageName)

    def writerBuilder = new OMEPyramidWriter.Builder(server)
        .region(x, y, width, height)
        .compression(compression)
        .parallelize()
        .tileSize(tilesize)
        .scaledDownsampling(outputDownsample, pyramidscaling)
        .channels(channelIndices as int[]) // <-- nur gewünschte Kanäle exportieren

    if (server.getMetadata().isRGB()) {
        writerBuilder.channelsInterleaved()
        println "Exporting as RGB with interleaved channels."
    } else {
        println "Exporting as multiplexed grayscale with separate channels."
    }

    writerBuilder.build().writePyramid(imagePath.getAbsolutePath())

    // === GeoJSON & Messwerte Export ===
    selectObjects([annotation])
    def annotGeoJsonPath = new File(annotDir, "annotation.geojson").getAbsolutePath()
    exportObjectsToGeoJson([annotation], annotGeoJsonPath, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")

    def hierarchy = getCurrentHierarchy()
    def children = hierarchy.getObjects(null, null).findAll { it.getParent() == annotation }
    def childGeoJsonPath = new File(annotDir, "cells.geojson").getAbsolutePath()
    exportObjectsToGeoJson(children, childGeoJsonPath, "EXCLUDE_MEASUREMENTS", "PRETTY_JSON", "FEATURE_COLLECTION")

    // Messwerte in Batches
    int batchIndex = 1
    children.collate(batchSize).each { batch ->
        def batchIDs = batch.collect { it.getID() }
        def batchFile = new File(annotDir, "measurements_batch_${batchIndex}.tsv")
        new MeasurementExporter()
            .imageList([projectEntry])
            .filter(obj -> batchIDs.contains(obj.getID()))
            .separator("\t")
            .exportType(PathCellObject)
            .exportMeasurements(batchFile)
        println "Exported batch ${batchIndex} with ${batch.size()} cells to ${batchFile.getName()}"
        batchIndex++
    }
}

println "All exports complete!"
