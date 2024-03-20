#include <gtest/gtest.h>
#include "common/string/stringutil.hh"

namespace common::test
{

class StringUtilTest : public ::testing::Test
{
protected:
    StringUtilTest() = default;
};

TEST_F(StringUtilTest, appendToCharSeparatedItems)
{
    {
        std::string s = "test,coll2,test_12d,a,last_coll";
        char sep = ',';
        std::string toAppend = " 0";
        
        common::StringUtil::appendToCharSeparatedItems(s, sep, toAppend);
        std::string exp = "test 0,coll2 0,test_12d 0,a 0,last_coll 0";
        
        ASSERT_EQ(s, exp);
    }
    {
        std::string s = "test";
        char sep = ',';
        std::string toAppend = " 0";
        
        common::StringUtil::appendToCharSeparatedItems(s, sep, toAppend);
        std::string exp = "test 0";
        
        ASSERT_EQ(s, exp);
    }
}

}  // namespace common::test
