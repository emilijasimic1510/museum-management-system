$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../backend/src/main/resources/collection-images'
$ids = @(436535,436532,437853,436121,435882)
$records = @()
for ($i=0; $i -lt $ids.Count; $i++) {
    $record = Invoke-RestMethod -Uri "https://collectionapi.metmuseum.org/public/collection/v1/objects/$($ids[$i])"
    if (!$record.isPublicDomain -or !$record.primaryImageSmall) { throw "No open image: $($ids[$i])" }
    $destination = Join-Path $assetRoot "met-$i.jpg"
    Invoke-WebRequest -Uri $record.primaryImageSmall -OutFile $destination
    $bitmap = [Drawing.Image]::FromFile([IO.Path]::GetFullPath($destination))
    try { if ($bitmap.Width -lt 100 -or $bitmap.Height -lt 100) { throw 'Invalid image dimensions' } } finally { $bitmap.Dispose() }
    $records += [ordered]@{file="met-$i.jpg";id=$record.objectID;title=$record.title;artist=$record.artistDisplayName;credit=$record.creditLine;source=$record.objectURL;image=$record.primaryImageSmall;license='CC0 1.0';licenseUrl='https://www.metmuseum.org/policies/image-resources';usage='Thematic reference image; not a photograph of the collection object';sha256=(Get-FileHash -Algorithm SHA256 $destination).Hash}
    $records | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $assetRoot 'sources.json')
}
$records | Select-Object file,title,artist | Format-Table
