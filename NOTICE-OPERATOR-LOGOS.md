# Operator logos

The files in `app/src/main/res/drawable-nodpi/op_*.webp` are the logos of Iranian
mobile operators and ISPs. They are **trademarks of their respective owners** and are
shown in MarbleDU only to identify the operator a connection belongs to. MarbleDU is
not affiliated with or endorsed by any of these companies. If you are a rights holder
and want a logo removed or replaced, please open an issue.

## Sources

| Logos | Source |
|---|---|
| MCI (Hamrah-e Aval) – 2025 identity | The "mci" sign of MCI's new brand identity, published February 2025 ([newmci.ir/brand-identity](https://newmci.ir/brand-identity); also on Wikimedia Commons as the public-domain `File:MCI logo (2025).svg`). Prepared from a publicly published copy. |
| Irancell – 2022 identity | The black oval of Irancell's current identity, filled with the brand yellow `#FFCC00` as Irancell's own logo rules require (black logo on yellow; [irancell.ir/en/p/4473/logo](https://irancell.ir/en/p/4473/logo)). Also on Wikimedia Commons as the public-domain `File:MTN Irancell Logo.svg`. Prepared from a publicly published copy. |
| Fanap Mobile | The official mark used on [fanapmobile.ir](https://fanapmobile.ir/) (site icon) and by the official "My FanapMobile" app. |
| Ariantel (Arin Tel) | The official Ariantel lockup published on the company's verified accounts (e.g. [virasty.com/Ariantel](https://virasty.com/Ariantel)); company site: ariantel.ir. |
| Fanava | Fanava Group's new identity, unveiled at Elecomp 28 (September 2025), from a publicly published copy. |
| RighTel, Taliya, ApTel, SamanTel, LotusTel, TeleKish, MTCE (Spadan) | [`MohammadTaghipour/pars_validator`](https://github.com/MohammadTaghipour/pars_validator) – `assets/icons/operators`, rendered again at high resolution from the SVG sources. |
| Shatel (also used for Shatel Mobile), Asiatech, HiWEB, Pars Online, MobinNet, SabaNet, Pishgaman, Zi-Tel | [`persianlabs/icons`](https://github.com/persianlabs/icons) – rasterised from the SVG sources. |
| TCI, Laser, Mabna Telecom, Datak, Afranet, MahanNet, Respina, TIC | Publicly published logo images (operator sites, logo directories). Afranet and TIC were replaced with cleaner copies in v۱.۶.۶; TCI, Mabna and Laser had their white backgrounds removed. |

**Preparation (v۱.۶.۶):** every logo went through one pipeline so the set looks
consistent: transparent background, empty margins trimmed, then **optical sizing**.
Each mark is scaled by the area its ink actually covers, not by its bounding box, so
wide wordmarks, square emblems and text-heavy logos carry the same visual weight inside
the square 256×256 canvas. The canvas also keeps a safe margin for the app's rounded
tile. Because the sizing is baked into the files, the app draws every logo at one
uniform scale (`OperatorLogos.FIT_SCALE`). Nothing is stretched or redrawn. Where a
source was a full lockup, only the official sign was cropped from it (MCI, TIC). The
Irancell oval is filled with the brand yellow, as described above.

The Fanap Mobile and Arin Tel files replace the invented placeholder vectors that
earlier versions shipped (`res/drawable/op_fanapmobile.xml`, `op_arintel.xml`); those
are deleted.

A rendered contact sheet of all bundled logos (white tile on the app's dark surface)
lives at `branding/operator-logos-preview.png`.

Petiak has no bundled logo yet (no trustworthy source was found). The app falls back to
the coloured letter badge for it and for any unknown ISP. Anarestan is no longer listed
as an operator (v۱.۶.۶).
