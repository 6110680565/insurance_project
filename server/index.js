const { urlencoded } = require("body-parser");
const express = require("express");
const app = express();
const listOfCustomer = [{"insuranceID":"A0840672", "countClaim": 2  },{"insuranceID":"A0840659", "countClaim": 0  }];

app.get('/getcount/:insuranceID', (req, res) => {
  let insuranceID = req.params.insuranceID;
  let data = {"insuranceID":"notfind"};
  listOfCustomer.forEach(element => {
    if (element["insuranceID"] == insuranceID){
      console.log(element);
      data = element;
    }
  });

  res.json(data);
  
  
  
});


app.listen(3000, () => {
  console.log("running server");
});

//URL("localhost:3000/getcount/nut") ==> {"name":"nut", "countClaim": 2  }