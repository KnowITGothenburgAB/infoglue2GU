/*
 * FCKeditor - The text editor for internet
 * Copyright (C) 2003-2004 Frederico Caldeira Knabben
 * 
 * Licensed under the terms of the GNU Lesser General Public License:
 * 		http://www.opensource.org/licenses/lgpl-license.php
 * 
 * For further information visit:
 * 		http://www.fckeditor.net/
 * 
 * File Name: sl.js
 * 	Slovenian language file.
 * 
 * Version:  2.0 RC3
 * Modified: 2005-03-01 17:26:18
 * 
 * File Authors:
 * 		Boris Volari�? (vol@rutka.net)
 */

var FCKLang =
{
// Language direction : "ltr" (left to right) or "rtl" (right to left).
Dir					: "ltr",

ToolbarCollapse		: "Zloži orodno vrstico",
ToolbarExpand		: "Razširi orodno vrstico",

// Toolbar Items and Context Menu
Save				: "Shrani",
NewPage				: "Nova stran",
Preview				: "Predogled",
Cut					: "Izreži",
Copy				: "Kopiraj",
Paste				: "Prilepi",
PasteText			: "Prilepi kot golo besedilo",
PasteWord			: "Prilepi iz Worda",
Print				: "Natisni",
SelectAll			: "Izberi vse",
RemoveFormat		: "Odstrani oblikovanje",
InsertLinkLbl		: "Povezava",
InsertLink			: "Vstavi/uredi povezavo",
RemoveLink			: "Odstrani povezavo",
Anchor				: "Vstavi/uredi zaznamek",
InsertImageLbl		: "Slika",
InsertImage			: "Vstavi/uredi sliko",
InsertTableLbl		: "Tabela",
InsertTable			: "Vstavi/uredi tabelo",
InsertLineLbl		: "Črta",
InsertLine			: "Vstavi vodoravno �?rto",
InsertSpecialCharLbl: "Posebni znak",
InsertSpecialChar	: "Vstavi posebni znak",
InsertSmileyLbl		: "Smeško",
InsertSmiley		: "Vstavi smeška",
About				: "O FCKeditorju",
Bold				: "Krepko",
Italic				: "Leže�?e",
Underline			: "Pod�?rtano",
StrikeThrough		: "Pre�?rtano",
Subscript			: "Podpisano",
Superscript			: "Nadpisano",
LeftJustify			: "Leva poravnava",
CenterJustify		: "Sredinska poravnava",
RightJustify		: "Desna poravnava",
BlockJustify		: "Obojestranska poravnava",
DecreaseIndent		: "Zmanjšaj zamik",
IncreaseIndent		: "Pove�?aj zamik",
Undo				: "Razveljavi",
Redo				: "Ponovi",
NumberedListLbl		: "Oštevil�?en seznam",
NumberedList		: "Vstavi/odstrani oštevil�?evanje",
BulletedListLbl		: "Ozna�?en seznam",
BulletedList		: "Vstavi/odstrani ozna�?evanje",
ShowTableBorders	: "Pokaži meje tabele",
ShowDetails			: "Pokaži podrobnosti",
Style				: "Slog",
FontFormat			: "Oblika",
Font				: "Pisava",
FontSize			: "Velikost",
TextColor			: "Barva besedila",
BGColor				: "Barva ozadja",
Source				: "Izvorna koda",
Find				: "Najdi",
Replace				: "Zamenjaj",
SpellCheck			: "Preveri �?rkovanje",
UniversalKeyboard	: "Ve�?jezi�?na tipkovnica",

Form			: "Obrazec",
Checkbox		: "Potrditveno polje",
RadioButton		: "Izbirno polje",
TextField		: "Vnosno polje",
Textarea		: "Vnosno obmo�?je",
HiddenField		: "Skrito polje",
Button			: "Gumb",
SelectionField	: "Spustni seznam",
ImageButton		: "Gumb s sliko",

// Context Menu
EditLink			: "Uredi povezavo",
InsertRow			: "Vstavi vrstico",
DeleteRows			: "Izbriši vrstice",
InsertColumn		: "Vstavi stolpec",
DeleteColumns		: "Izbriši stolpce",
InsertCell			: "Vstavi celico",
DeleteCells			: "Izbriši celice",
MergeCells			: "Združi celice",
SplitCell			: "Razdeli celico",
CellProperties		: "Lastnosti celice",
TableProperties		: "Lastnosti tabele",
ImageProperties		: "Lastnosti slike",

AnchorProp			: "Lastnosti zaznamka",
ButtonProp			: "Lastnosti gumba",
CheckboxProp		: "Lastnosti potrditvenega polja",
HiddenFieldProp		: "Lastnosti skritega polja",
RadioButtonProp		: "Lastnosti izbirnega polja",
ImageButtonProp		: "Lastnosti gumba s sliko",
TextFieldProp		: "Lastnosti vnosnega polja",
SelectionFieldProp	: "Lastnosti spustnega seznama",
TextareaProp		: "Lastnosti vnosnega obmo�?ja",
FormProp			: "Lastnosti obrazca",

FontFormats			: "Navaden;Oblikovan;Napis;Naslov 1;Naslov 2;Naslov 3;Naslov 4;Naslov 5;Naslov 6",	// 2.0: The last entry has been added.

// Alerts and Messages
ProcessingXHTML		: "Obdelujem XHTML. Prosim po�?akajte...",
Done				: "Narejeno",
PasteWordConfirm	: "Izgleda, da želite prilepiti besedilo iz Worda. Ali ga želite o�?istiti, preden ga prilepite?",
NotCompatiblePaste	: "Ta ukaz deluje le v Internet Explorerje razli�?ice 5.5 ali višje. Ali želite prilepiti brez �?iš�?enja?",
UnknownToolbarItem	: "Neznan element orodne vrstice \"%1\"",
UnknownCommand		: "Neznano ime ukaza \"%1\"",
NotImplemented		: "Ukaz ni izdelan",
UnknownToolbarSet	: "Skupina orodnih vrstic \"%1\" ne obstoja",

// Dialogs
DlgBtnOK			: "V redu",
DlgBtnCancel		: "Prekli�?i",
DlgBtnClose			: "Zapri",
DlgBtnBrowseServer	: "Prebrskaj na strežniku",
DlgAdvancedTag		: "Napredno",
DlgOpOther			: "&lt;Ostalo&gt;",

// General Dialogs Labels
DlgGenNotSet		: "&lt;ni postavljen&gt;",
DlgGenId			: "Id",
DlgGenLangDir		: "Smer jezika",
DlgGenLangDirLtr	: "Od leve proti desni (LTR)",
DlgGenLangDirRtl	: "Od desne proti levi (RTL)",
DlgGenLangCode		: "Oznaka jezika",
DlgGenAccessKey		: "Vstopno geslo",
DlgGenName			: "Ime",
DlgGenTabIndex		: "Številka tabulatorja",
DlgGenLongDescr		: "Dolg opis URL-ja",
DlgGenClass			: "Razred stilne predloge",
DlgGenTitle			: "Predlagani naslov",
DlgGenContType		: "Predlagani tip vsebine (content-type)",
DlgGenLinkCharset	: "Kodna tabela povezanega vira",
DlgGenStyle			: "Slog",

// Image Dialog
DlgImgTitle			: "Lastnosti slike",
DlgImgInfoTab		: "Podatki o sliki",
DlgImgBtnUpload		: "Pošlji na strežnik",
DlgImgURL			: "URL",
DlgImgUpload		: "Pošlji",
DlgImgAlt			: "Nadomestno besedilo",
DlgImgWidth			: "Širina",
DlgImgHeight		: "Višina",
DlgImgLockRatio		: "Zakleni razmerje",
DlgBtnResetSize		: "Ponastavi velikost",
DlgImgBorder		: "Obroba",
DlgImgHSpace		: "Vodoravni razmik",
DlgImgVSpace		: "Navpi�?ni razmik",
DlgImgAlign			: "Poravnava",
DlgImgAlignLeft		: "Levo",
DlgImgAlignAbsBottom: "Popolnoma na dno",
DlgImgAlignAbsMiddle: "Popolnoma v sredino",
DlgImgAlignBaseline	: "Na osnovno �?rto",
DlgImgAlignBottom	: "Na dno",
DlgImgAlignMiddle	: "V sredino",
DlgImgAlignRight	: "Desno",
DlgImgAlignTextTop	: "Besedilo na vrh",
DlgImgAlignTop		: "Na vrh",
DlgImgPreview		: "Predogled",
DlgImgAlertUrl		: "Vnesite URL slike",

// Link Dialog
DlgLnkWindowTitle	: "Povezava",
DlgLnkInfoTab		: "Podatki o povezavi",
DlgLnkTargetTab		: "Cilj",

DlgLnkType			: "Vrsta povezave",
DlgLnkTypeURL		: "URL",
DlgLnkTypeAnchor	: "Zaznamek na tej strani",
DlgLnkTypeEMail		: "Elektronski naslov",
DlgLnkProto			: "Protokol",
DlgLnkProtoOther	: "&lt;drugo&gt;",
DlgLnkURL			: "URL",
DlgLnkAnchorSel		: "Izberi zaznamek",
DlgLnkAnchorByName	: "Po imenu zaznamka",
DlgLnkAnchorById	: "Po ID-ju elementa",
DlgLnkNoAnchors		: "&lt;V tem dokumentu ni zaznamkov&gt;",
DlgLnkEMail			: "Elektronski naslov",
DlgLnkEMailSubject	: "Predmet sporo�?ila",
DlgLnkEMailBody		: "Vsebina sporo�?ila",
DlgLnkUpload		: "Prenesi",
DlgLnkBtnUpload		: "Pošlji na strežnik",

DlgLnkTarget		: "Cilj",
DlgLnkTargetFrame	: "&lt;okvir&gt;",
DlgLnkTargetPopup	: "&lt;pojavno okno&gt;",
DlgLnkTargetBlank	: "Novo okno (_blank)",
DlgLnkTargetParent	: "Starševsko okno (_parent)",
DlgLnkTargetSelf	: "Isto okno (_self)",
DlgLnkTargetTop		: "Najvišje okno (_top)",
DlgLnkTargetFrameName	: "Ime ciljnega okvirja",
DlgLnkPopWinName	: "Ime pojavnega okna",
DlgLnkPopWinFeat	: "Zna�?ilnosti pojavnega okna",
DlgLnkPopResize		: "Spremenljive velikosti",
DlgLnkPopLocation	: "Naslovna vrstica",
DlgLnkPopMenu		: "Menijska vrstica",
DlgLnkPopScroll		: "Drsniki",
DlgLnkPopStatus		: "Vrstica stanja",
DlgLnkPopToolbar	: "Orodna vrstica",
DlgLnkPopFullScrn	: "Celozaslonska slika (IE)",
DlgLnkPopDependent	: "Podokno (Netscape)",
DlgLnkPopWidth		: "Širina",
DlgLnkPopHeight		: "Višina",
DlgLnkPopLeft		: "Lega levo",
DlgLnkPopTop		: "Lega na vrhu",

DlnLnkMsgNoUrl		: "Vnesite URL povezave",
DlnLnkMsgNoEMail	: "Vnesite elektronski naslov",
DlnLnkMsgNoAnchor	: "Izberite zaznamek",

// Color Dialog
DlgColorTitle		: "Izberite barvo",
DlgColorBtnClear	: "Po�?isti",
DlgColorHighlight	: "Ozna�?i",
DlgColorSelected	: "Izbrano",

// Smiley Dialog
DlgSmileyTitle		: "Vstavi smeška",

// Special Character Dialog
DlgSpecialCharTitle	: "Izberi posebni znak",

// Table Dialog
DlgTableTitle		: "Lastnosti tabele",
DlgTableRows		: "Vrstice",
DlgTableColumns		: "Stolpci",
DlgTableBorder		: "Velikost obrobe",
DlgTableAlign		: "Poravnava",
DlgTableAlignNotSet	: "<Ni nastavljeno>",
DlgTableAlignLeft	: "Levo",
DlgTableAlignCenter	: "Sredinsko",
DlgTableAlignRight	: "Desno",
DlgTableWidth		: "Širina",
DlgTableWidthPx		: "pik",
DlgTableWidthPc		: "procentov",
DlgTableHeight		: "Višina",
DlgTableCellSpace	: "Razmik med celicami",
DlgTableCellPad		: "Polnilo med celicami",
DlgTableCaption		: "Naslov",

// Table Cell Dialog
DlgCellTitle		: "Lastnosti celice",
DlgCellWidth		: "Širina",
DlgCellWidthPx		: "pik",
DlgCellWidthPc		: "procentov",
DlgCellHeight		: "Višina",
DlgCellWordWrap		: "Pomikanje besedila",
DlgCellWordWrapNotSet	: "<Ni nastavljeno>",
DlgCellWordWrapYes	: "Da",
DlgCellWordWrapNo	: "Ne",
DlgCellHorAlign		: "Vodoravna poravnava",
DlgCellHorAlignNotSet	: "<Ni nastavljeno>",
DlgCellHorAlignLeft	: "Levo",
DlgCellHorAlignCenter	: "Sredinsko",
DlgCellHorAlignRight: "Desno",
DlgCellVerAlign		: "Navpi�?na poravnava",
DlgCellVerAlignNotSet	: "<Ni nastavljeno>",
DlgCellVerAlignTop	: "Na vrh",
DlgCellVerAlignMiddle	: "V sredino",
DlgCellVerAlignBottom	: "Na dno",
DlgCellVerAlignBaseline	: "Na osnovno �?rto",
DlgCellRowSpan		: "Spojenih vrstic (row-span)",
DlgCellCollSpan		: "Spojenih stolpcev (col-span)",
DlgCellBackColor	: "Barva ozadja",
DlgCellBorderColor	: "Barva obrobe",
DlgCellBtnSelect	: "Izberi...",

// Find Dialog
DlgFindTitle		: "Najdi",
DlgFindFindBtn		: "Najdi",
DlgFindNotFoundMsg	: "Navedeno besedilo ni bilo najdeno.",

// Replace Dialog
DlgReplaceTitle			: "Zamenjaj",
DlgReplaceFindLbl		: "Najdi:",
DlgReplaceReplaceLbl	: "Zamenjaj z:",
DlgReplaceCaseChk		: "Razlikuj velike in male �?rke",
DlgReplaceReplaceBtn	: "Zamenjaj",
DlgReplaceReplAllBtn	: "Zamenjaj vse",
DlgReplaceWordChk		: "Samo cele besede",

// Paste Operations / Dialog
PasteErrorPaste	: "Varnostne nastavitve brskalnika ne dopuš�?ajo samodejnega lepljenja. Uporabite kombinacijo tipk na tipkovnici (Ctrl+V).",
PasteErrorCut	: "Varnostne nastavitve brskalnika ne dopuš�?ajo samodejnega izrezovanja. Uporabite kombinacijo tipk na tipkovnici (Ctrl+X).",
PasteErrorCopy	: "Varnostne nastavitve brskalnika ne dopuš�?ajo samodejnega kopiranja. Uporabite kombinacijo tipk na tipkovnici (Ctrl+C).",

PasteAsText		: "Prilepi kot golo besedilo",
PasteFromWord	: "Prilepi iz Worda",

DlgPasteMsg		: "Ni bilo mogo�?e izvesti lepljenja zaradi <STRONG>varnostnih nastavitev</STRONG> vašega brskalnika.<BR>Prilepite v slede�?e okno s kombinacijo tipk na tipkovnici (<STRONG>Ctrl+V</STRONG>) in pritisnite <STRONG>V redu</STRONG>.",

// Color Picker
ColorAutomatic	: "Samodejno",
ColorMoreColors	: "Ve�? barv...",

// Document Properties
DocProps		: "Lastnosti dokumenta",

// Anchor Dialog
DlgAnchorTitle		: "Lastnosti zaznamka",
DlgAnchorName		: "Ime zaznamka",
DlgAnchorErrorName	: "Prosim vnesite ime zaznamka",

// Speller Pages Dialog
DlgSpellNotInDic		: "Ni v slovarju",
DlgSpellChangeTo		: "Spremeni v",
DlgSpellBtnIgnore		: "Prezri",
DlgSpellBtnIgnoreAll	: "Prezri vse",
DlgSpellBtnReplace		: "Zamenjaj",
DlgSpellBtnReplaceAll	: "Zamenjaj vse",
DlgSpellBtnUndo			: "Razveljavi",
DlgSpellNoSuggestions	: "- Ni predlogov -",
DlgSpellProgress		: "Preverjanje �?rkovanja se izvaja...",
DlgSpellNoMispell		: "Črkovanje je kon�?ano: Brez napak",
DlgSpellNoChanges		: "Črkovanje je kon�?ano: Nobena beseda ni bila spremenjena",
DlgSpellOneChange		: "Črkovanje je kon�?ano: Spremenjena je bila ena beseda",
DlgSpellManyChanges		: "Črkovanje je kon�?ano: Spremenjenih je bilo %1 besed",

IeSpellDownload			: "Črkovalnik ni nameš�?en. Ali ga želite prenesti sedaj?",

// Button Dialog
DlgButtonText	: "Besedilo (Vrednost)",
DlgButtonType	: "Tip",

// Checkbox and Radio Button Dialogs
DlgCheckboxName		: "Ime",
DlgCheckboxValue	: "Vrednost",
DlgCheckboxSelected	: "Izbrano",

// Form Dialog
DlgFormName		: "Ime",
DlgFormAction	: "Akcija",
DlgFormMethod	: "Metoda",

// Select Field Dialog
DlgSelectName		: "Ime",
DlgSelectValue		: "Vrednost",
DlgSelectSize		: "Velikost",
DlgSelectLines		: "vrstic",
DlgSelectChkMulti	: "Dovoli izbor ve�?ih vrstic",
DlgSelectOpAvail	: "Razpoložljive izbire",
DlgSelectOpText		: "Besedilo",
DlgSelectOpValue	: "Vrednost",
DlgSelectBtnAdd		: "Dodaj",
DlgSelectBtnModify	: "Spremeni",
DlgSelectBtnUp		: "Gor",
DlgSelectBtnDown	: "Dol",
DlgSelectBtnSetValue : "Postavi kot privzeto izbiro",
DlgSelectBtnDelete	: "Izbriši",

// Textarea Dialog
DlgTextareaName	: "Ime",
DlgTextareaCols	: "Stolpcev",
DlgTextareaRows	: "Vrstic",

// Text Field Dialog
DlgTextName			: "Ime",
DlgTextValue		: "Vrednost",
DlgTextCharWidth	: "Dolžina",
DlgTextMaxChars		: "Najve�?je število znakov",
DlgTextType			: "Tip",
DlgTextTypeText		: "Besedilo",
DlgTextTypePass		: "Geslo",

// Hidden Field Dialog
DlgHiddenName	: "Ime",
DlgHiddenValue	: "Vrednost",

// Bulleted List Dialog
BulletedListProp	: "Lastnosti ozna�?enega seznama",
NumberedListProp	: "Lastnosti oštevil�?enega seznama",
DlgLstType			: "Tip",
DlgLstTypeCircle	: "Pikica",
DlgLstTypeDisk		: "Krožec",
DlgLstTypeSquare	: "Kvadratek",
DlgLstTypeNumbers	: "Številke (1, 2, 3)",
DlgLstTypeLCase		: "Male �?rke (a, b, c)",
DlgLstTypeUCase		: "Velike �?rke (A, B, C)",
DlgLstTypeSRoman	: "Male rimske številke (i, ii, iii)",
DlgLstTypeLRoman	: "Velike rimske številke (I, II, III)",

// Document Properties Dialog
DlgDocGeneralTab	: "Splošno",
DlgDocBackTab		: "Ozadje",
DlgDocColorsTab		: "Barve in zamiki",
DlgDocMetaTab		: "Meta podatki",

DlgDocPageTitle		: "Naslov strani",
DlgDocLangDir		: "Smer jezika",
DlgDocLangDirLTR	: "Od leve proti desni (LTR)",
DlgDocLangDirRTL	: "Od desne proti levi (RTL)",
DlgDocLangCode		: "Oznaka jezika",
DlgDocCharSet		: "Kodna tabela",
DlgDocCharSetOther	: "Druga kodna tabela",

DlgDocDocType		: "Glava tipa dokumenta",
DlgDocDocTypeOther	: "Druga glava tipa dokumenta",
DlgDocIncXHTML		: "Vstavi XHTML deklaracije",
DlgDocBgColor		: "Barva ozadja",
DlgDocBgImage		: "URL slike za ozadje",
DlgDocBgNoScroll	: "Nepremi�?no ozadje",
DlgDocCText			: "Besedilo",
DlgDocCLink			: "Povezava",
DlgDocCVisited		: "Obiskana povezava",
DlgDocCActive		: "Aktivna povezava",
DlgDocMargins		: "Zamiki strani",
DlgDocMaTop			: "Na vrhu",
DlgDocMaLeft		: "Levo",
DlgDocMaRight		: "Desno",
DlgDocMaBottom		: "Spodaj",
DlgDocMeIndex		: "Klju�?ne besede (lo�?ene z vejicami)",
DlgDocMeDescr		: "Opis strani",
DlgDocMeAuthor		: "Avtor",
DlgDocMeCopy		: "Avtorske pravice",
DlgDocPreview		: "Predogled",

// About Dialog
DlgAboutAboutTab	: "Vizitka",
DlgAboutBrowserInfoTab	: "Informacije o brskalniku",
DlgAboutVersion		: "razli�?ica",
DlgAboutLicense		: "Pravica za uporabo pod pogoji GNU Lesser General Public License",
DlgAboutInfo		: "Za ve�? informacij obiš�?ite"
}