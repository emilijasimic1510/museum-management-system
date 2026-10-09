$ErrorActionPreference = 'Stop'
$assetRoot = Join-Path $PSScriptRoot '../backend/src/main/resources/collection-images'
New-Item -ItemType Directory -Force -Path $assetRoot | Out-Null
$ids = @(66303,719,28560,4368,33053,192876,11,62892)
$records = @()
for ($i=0; $i -lt $ids.Count; $i++) {
    $record = (Invoke-RestMethod -Uri "https://api.artic.edu/api/v1/artworks/$($ids[$i])?fields=id,title,artist_display,image_id,is_public_domain,date_display,credit_line").data
    if (!$record.is_public_domain -or !$record.image_id) { throw 'Image has no public-domain clearance' }
    $url = "https://www.artic.edu/iiif/2/$($record.image_id)/full/843,/0/default.jpg"
    $destination = Join-Path $assetRoot "$i.jpg"
    Invoke-WebRequest -Uri $url -OutFile $destination
    $bytes = [IO.File]::ReadAllBytes($destination)
    if ($bytes.Length -lt 1000 -or $bytes[0] -ne 255 -or $bytes[1] -ne 216) { throw "Invalid JPEG: $url" }
    $records += [ordered]@{group=$i;id=$record.id;title=$record.title;artist=$record.artist_display;date=$record.date_display;credit=$record.credit_line;source="https://www.artic.edu/artworks/$($record.id)";image=$url;license='CC0 1.0';usage='Thematic illustration; not a photograph of the fictional collection object';bytes=$bytes.Length}
}
$records | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $assetRoot 'sources.json')
$records | Select-Object group,title,bytes | Format-Table
