

<style type="text/css">
		table.ps_content_browse_viewtable {
			font-family:Lucida Grande, Verdana;
			font-size:1.0em;
			width:100%;
			border:0px solid #ccc;
			border-collapse:collapse;
			cursor:default;
		}
</style>
<table class="ps_content_browse_viewtable">
	<tr>
	   <td height="90%" width="100%">
			<table height="100%" width="100%" style="border: 0px light gray">
				<tr>
				   <td height="100%" width="85%">
						<span style="font-family: Arial;font-weight: Bold;font-size: 12px;color: #000000;overflow: hidden;" align="left">Snippets</span>
						<div dojoType="ContentPane" id="ps.snippet.picker.wgtSnippetDisplayDiv" style="background-color: white; border: 1px solid #6290D2; width: 600px; height: 300px; overflow:auto">
						</div>
				   </td>
				   <td align="left" height="100%" width="15%" left-margin="10px">
					   <table id="ps.snippet.picker.tblCreateSnippetBtns" style="border: 1px light gray; width:100%; padding-left:10px;">
							<tr>
							   <td align="left">
									Place Where
							   </td>
							</tr>
							<tr>
							   <td align="left">
									<input type="radio" id="ps.snippet.picker.placeWhereRadio" name="ps.snippet.picker.placeWhereRadio" value="before" checked="true"/> Before<br>
							   </td>
							</tr>
							<tr>
							   <td align="left">
									<input type="radio" id="ps.snippet.picker.placeWhereRadio" name="ps.snippet.picker.placeWhereRadio" value="after"/> After<br>
							   </td>
							</tr>
							<tr>
							   <td align="left">
									<input type="radio" id="ps.snippet.picker.placeWhereRadio" name="ps.snippet.picker.placeWhereRadio" value="replace"/> Replace				
							   </td>
							</tr>
					   </table>
					   <table id="ps.snippet.picker.tblRemoveSnippetBtns" style="border: 0px light gray; width:100%; padding-left:10px;">
							<tr>
							   <td align="left">
									<br/>
									<button dojoType="ps:PSButton" id="ps.snippet.picker.wgtButtonSelectAll" style="width=100px">Select All&nbsp;&nbsp;</button>									   
								</td>
							</tr>
							<tr>
							   <td align="left">
								   <button dojoType="ps:PSButton" id="ps.snippet.picker.wgtButtonDeselectAll" style="width=100px">Deselct All</button>
							   </td>
							</tr>
					   </table>
				   </td>
				</tr>
			</table>
	   </td>
	</tr>
	<tr>
	   <td height="10%">
	   <table style="border: 0px light gray; width:100%; padding-top:10px">
		   <tr>
			   <td align="left" width="50%">
				   <button dojoType="ps:PSButton" id="ps.snippet.picker.wgtButtonShowTitles">Show Titles</button>
			   </td>
			   <td align="right" width="50%">
				<table>
					<tr>
					   <td align="right">
						   <button dojoType="ps:PSButton" id="ps.snippet.picker.wgtButtonSelect">Remove</button>
					   </td>
					   <td align="left">
						   <button dojoType="ps:PSButton" id="ps.snippet.picker.wgtButtonCancel">Cancel</button>
					   </td>
					</tr>
				</table>
			   </td>
			</tr>
	   </table>
	   </td>
	</tr>
</table>
