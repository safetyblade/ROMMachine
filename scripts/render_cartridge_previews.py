"""Render native VectorDrawable layers for visual review (not Android screenshots).

Optional dependency: cairosvg. Runtime Android does not use this script or its PNGs.
"""
from pathlib import Path
import xml.etree.ElementTree as ET
import cairosvg

ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'
S = 'http://www.w3.org/2000/svg'
ET.register_namespace('', S)


def layer(parent, edition, name, **bounds):
    source = ROOT / f'lemuroid-app/src/main/res/drawable/cartdeck_{edition}_{name}.xml'
    native = ET.parse(source).getroot()
    group = ET.SubElement(parent, f'{{{S}}}svg', {
        'viewBox': f"0 0 {native.get(A + 'viewportWidth')} {native.get(A + 'viewportHeight')}",
        **{key: str(value) for key, value in bounds.items()},
    })
    for node in native:
        attrs = {'d': node.get(A + 'pathData')}
        for android, svg in (
            ('fillColor', 'fill'), ('strokeColor', 'stroke'), ('strokeWidth', 'stroke-width'),
            ('strokeAlpha', 'stroke-opacity'), ('fillAlpha', 'fill-opacity'),
            ('strokeLineCap', 'stroke-linecap'), ('strokeLineJoin', 'stroke-linejoin'),
        ):
            value = node.get(A + android)
            if value is not None:
                attrs[svg] = 'none' if value == '#00000000' else value
        ET.SubElement(group, f'{{{S}}}path', attrs)


for edition in ('original', 'pink'):
    svg = ET.Element(f'{{{S}}}svg', {'viewBox': '0 0 440 430', 'width': '440', 'height': '430'})
    layer(svg, edition, 'front_base', x=10, y=10, width=320, height=390)
    layer(svg, edition, 'fallback_atlas', x=50, y=123, width=240, height=200)
    layer(svg, edition, 'front_overlay', x=10, y=10, width=320, height=390)
    layer(svg, edition, 'cslot_master', x=50, y=48, width=40, height=40)
    layer(svg, edition, 'spine_base', x=340, y=35, width=94, height=340)
    layer(svg, edition, 'cslot_master', x=373, y=70, width=28, height=28)
    for x, y, text, extra in ((110, 75, 'CARTDECK', {}), (264, 360, 'GB', {'text-anchor': 'middle'}), (387, 325, 'GB', {'text-anchor': 'middle'})):
        element = ET.SubElement(svg, f'{{{S}}}text', {'x': str(x), 'y': str(y), 'font-family': 'sans-serif', 'font-size': '14', 'fill': '#EDF6F2' if edition == 'original' else '#FFF3F7', **extra})
        element.text = text
    output = ROOT / f'docs/engineering/p1-a1/previews/{edition}-native-resources.png'
    cairosvg.svg2png(bytestring=ET.tostring(svg), write_to=str(output), scale=2)
