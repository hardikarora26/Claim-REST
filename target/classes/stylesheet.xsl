<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:x="http://www.mitchell.com/examples/claim">
<xsl:template match="/">
  <html>
  <body>
    <h2>Claim Infomation</h2>
    <table border="1">
      <tr bgcolor="#9acd32">
        <th>ClaimNumber</th>
        <th>ClaimantFirstName</th>
		<th>ClaimantLastName</th>
		<th>Status</th>
		<th>LossDate</th>
		<th>CauseOfLoss</th>
		<th>ReportedDate</th>
		<th>LossDescription</th>
		<th>AssignedAdjusterID</th>
      </tr>
      <tr>
        <td><xsl:value-of select="x:MitchellClaim/x:ClaimNumber"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:ClaimantFirstName"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:ClaimantLastName"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:Status"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:LossDate"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:LossInfo/x:CauseOfLoss"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:LossInfo/x:ReportedDate"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:LossInfo/x:LossDescription"/></td>
		<td><xsl:value-of select="x:MitchellClaim/x:AssignedAdjusterID"/></td>
      </tr>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>

