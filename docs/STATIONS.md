# Station data

The offline station directory is derived from [Suica Viewer's station codes](https://github.com/soltia48/suica-viewer/blob/e1fde64fb1d255bf9ca658e6c4cba9078ce26ae2/assets/station_codes.csv), distributed under the [MIT license](../app/src/main/assets/stations/LICENSE.txt).

The bundled table contains 6,954 unambiguous area, line, and station code combinations. It retains Japanese operator, line, and station names. Notes are omitted, hexadecimal codes are stored as decimal integers, and two conflicting code combinations are omitted.

Entry and exit areas are decoded separately. Unknown codes remain labelled as unknown stations; they are never matched to a station in a different area. Bus and purchase records do not use the rail station directory. Existing saved transactions retain their original card bytes and can resolve station names without another scan.
